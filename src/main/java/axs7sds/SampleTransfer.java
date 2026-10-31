package axs7sds;

import java.util.Arrays;
import java.util.HexFormat;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.SysexMessage;

import axs7sds.SampleDump.SampleBlock;
import axs7sds.SampleDump.SampleHeader;
import axs7sds.midi.ByteConverter;

/**
 * Transfers samples to and from an Akai X7000/S700 with its sample dump protocol.
 *
 * A dump is one long sysex message: a 19 byte header (without F7), a number of 122 byte blocks and a final F7.
 * The receiving side sends an ACK (F0 7E 7F F7) after the header and after each block, and the sender waits for
 * it before sending the next block.
 */
public class SampleTransfer implements AutoCloseable
{
  static final byte[] ACK                = {(byte) 0xF0, 0x7E, 0x7F, (byte) 0xF7};
  static final byte[] REQUEST_SAMPLEDUMP = {(byte) 0xF0, 0x7E, 0x0, 0x0, 0x0, (byte) 0xF7};
  static final int    HEADER_LENGTH      = 19;
  static final int    BLOCK_LENGTH       = SampleBlock.NUM_SAMPLES_IN_BLOCK * 2 + 2;
  static final long   ACK_INTERVAL_MS    = 50;
  static final long   HEADER_TIMEOUT     = 20_000;
  static final long   DATA_TIMEOUT       = 30_000;
  static final long   END_TIMEOUT        = 2_000;

  public interface Progress
  {
    void update(String note, int value, int max);

    default boolean isCanceled()
    {
      return false;
    }
  }

  /**
   * @param period the sampling period in ns from the header, the X7000 has been seen sending garbage here
   */
  public record ReceivedSample(SampleHeader header, byte[] headerBytes, int period, double[] data, int blocks,
      int checksumErrors)
  {
    public boolean validPeriod()
    {
      return SampleHeader.validPeriod(period);
    }
  }

  final LinkedBlockingQueue<MidiMessage> messages = new LinkedBlockingQueue<>();
  private final boolean                  debug;
  private final boolean                  trace;
  private MidiDevice                     midiIn;
  private MidiDevice                     midiOut;

  public SampleTransfer(MidiDevice.Info in, MidiDevice.Info out, boolean debug, boolean trace)
      throws MidiUnavailableException
  {
    this(debug, trace);
    try
    {
      // Open the input first so the start of a reply is not lost
      midiIn = MidiSystem.getMidiDevice(in);
      midiIn.getTransmitter().setReceiver(new MessageQueueReceiver());
      midiIn.open();
      midiOut = MidiSystem.getMidiDevice(out);
      midiOut.open();
    }
    catch (MidiUnavailableException | RuntimeException e)
    {
      close();
      throw e;
    }
  }

  /**
   * Without midi devices, for testing the parsing of incoming data
   */
  SampleTransfer(boolean debug, boolean trace)
  {
    this.debug = debug;
    this.trace = trace;
  }

  @Override
  public void close()
  {
    if (midiOut != null && midiOut.isOpen())
    {
      midiOut.close();
    }
    if (midiIn != null && midiIn.isOpen())
    {
      midiIn.close();
    }
  }

  class MessageQueueReceiver implements Receiver
  {
    @Override
    public void send(MidiMessage message, long timeStamp)
    {
      if (message instanceof SysexMessage)
      {
        debug("midi in: sysex, " + message.getLength() + " bytes");
      }
      else
      {
        debug("midi in: " + HexFormat.of().formatHex(message.getMessage()));
      }
      messages.add(message);
    }

    @Override
    public void close()
    {}
  }

  /**
   * @param data the sample data, scaled to unsigned values for the number of bits in the header
   */
  public void transmit(SampleHeader header, int[] data, Progress progress) throws Exception
  {
    messages.clear();
    int numBlocks = header.numBlocks();
    sendSysEx("Sending sample header", header.getBytes());
    for (int i = 0; i < numBlocks; i++)
    {
      progress.update("packet #" + i, i + 1, numBlocks + 1);
      sendSysEx("Sending block# " + i, new SampleBlock(header, i, data).getBytes(i));
      byte[] ack = receiveSysEx(1000);
      if (ack == null)
      {
        throw new Exception("no ack received!");
      }
      if (i == 0)
      {
        debug("first ack from the sampler: " + HexFormat.of().formatHex(ack));
      }
    }
    sendSysEx("End", new byte[]{(byte) 0xF7});
    progress.update("Finished", numBlocks + 1, numBlocks + 1);
  }

  /**
   * @param sampleNumber 0-5
   */
  public ReceivedSample receive(int sampleNumber, Progress progress) throws Exception
  {
    messages.clear();
    byte[] request = REQUEST_SAMPLEDUMP.clone();
    request[3] = (byte) sampleNumber;
    sendSysEx("Requesting sample dump", request);
    SysexByteStream stream = new SysexByteStream(progress);
    ScheduledExecutorService acker = startAcker(stream.created);
    try
    {
      return readDump(stream, progress);
    }
    finally
    {
      acker.shutdownNow();
      acker.awaitTermination(1, TimeUnit.SECONDS);
    }
  }

  /**
   * The sampler waits for an ACK after the header and after each block, but the midi driver holds back the
   * incoming data until its buffer is full or F7 arrives (the header has no F7), so the header and blocks can't be
   * seen in time to answer them. The sampler then times out, ends the dump with F7 and stops. Instead ACKs are sent
   * at a fixed pace, slightly slower than the time it takes to send a block (122 bytes at 31250 baud = 39 ms).
   */
  private ScheduledExecutorService startAcker(long startTime) throws Exception
  {
    Receiver ackReceiver = midiOut.getReceiver();
    SysexMessage ack = new SysexMessage(ACK, ACK.length);
    AtomicInteger count = new AtomicInteger();
    ScheduledExecutorService acker = Executors.newSingleThreadScheduledExecutor();
    acker.scheduleAtFixedRate(() -> {
      try
      {
        ackReceiver.send(ack, -1);
        int n = count.incrementAndGet();
        if (n % 50 == 1)
        {
          debug(String.format("%6d ms: sent ack #%d", System.currentTimeMillis() - startTime, n));
        }
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }, ACK_INTERVAL_MS, ACK_INTERVAL_MS, TimeUnit.MILLISECONDS);
    return acker;
  }

  ReceivedSample readDump(SysexByteStream stream, Progress progress) throws Exception
  {
    byte[] headerBytes = stream.next(HEADER_LENGTH, HEADER_TIMEOUT);
    if ((headerBytes[0] & 0xFF) != 0xF0 || headerBytes[1] != 0x7E || headerBytes[2] != 0x01)
    {
      throw new Exception("Expected a sample dump header, but received: " + HexFormat.of().formatHex(headerBytes));
    }
    int period = ByteConverter.intFrom3Bytes(headerBytes, 6);
    if (ByteConverter.intFrom3Bytes(headerBytes, 9) <= 0)
    {
      throw new Exception("Invalid sample dump header: " + HexFormat.of().formatHex(headerBytes));
    }
    SampleHeader sampleHeader = SampleHeader.fromBytes(headerBytes);
    debug("Received header: " + HexFormat.of().formatHex(headerBytes));
    debug("Received header: " + sampleHeader + ", period=" + period + "ns");

    int expectedBlocks = (sampleHeader.numSamples() + SampleBlock.NUM_SAMPLES_IN_BLOCK - 1)
        / SampleBlock.NUM_SAMPLES_IN_BLOCK;
    double[] received = new double[SampleBlock.NUM_BLOCKS * SampleBlock.NUM_SAMPLES_IN_BLOCK];
    int blockCount = 0;
    int checksumErrors = 0;
    while (true)
    {
      int first;
      try
      {
        first = stream.next(blockCount < expectedBlocks ? DATA_TIMEOUT : END_TIMEOUT);
      }
      catch (TimeoutException e)
      {
        if (blockCount == 0)
        {
          throw new TimeoutException("No sample data received after the header");
        }
        // keep what has been received so far
        debug("Timed out after receiving " + blockCount + " of " + expectedBlocks + " blocks");
        break;
      }
      if (first == 0xF7 && blockCount >= expectedBlocks)
      {
        break; // end of dump
      }
      if (first == 0xF0 || first == 0xF7)
      {
        // Sysex framing between blocks (e.g. the header terminated by F7 as a message of its own), skip it
        debug("Skipping " + String.format("%02x", first) + " before block " + blockCount);
        continue;
      }
      byte[] rest = stream.next(BLOCK_LENGTH - 1, DATA_TIMEOUT);
      byte[] block = new byte[BLOCK_LENGTH];
      block[0] = (byte) first;
      System.arraycopy(rest, 0, block, 1, rest.length);
      for (int i = 0; i < block.length; i++)
      {
        if ((block[i] & 0x80) != 0)
        {
          throw new Exception("Unexpected status byte in block " + blockCount + ", data is out of sync: "
              + HexFormat.of().formatHex(block));
        }
      }
      if (block[0] != blockCount % 128)
      {
        throw new Exception("Expected block number " + (blockCount % 128) + " but got " + block[0]
            + ", data is out of sync");
      }
      if (blockCount >= SampleBlock.NUM_BLOCKS)
      {
        throw new Exception("Received more than " + SampleBlock.NUM_BLOCKS + " blocks");
      }
      byte chk = 0;
      for (int b = 1; b <= SampleBlock.NUM_SAMPLES_IN_BLOCK * 2; b++)
      {
        chk = (byte) (chk ^ block[b]);
      }
      if (chk != block[block.length - 1])
      {
        checksumErrors++;
        debug("Checksum error in block " + blockCount);
      }
      for (int s = 0; s < SampleBlock.NUM_SAMPLES_IN_BLOCK; s++)
      {
        // 12 bit offset binary, 2048 = 0
        int value = ByteConverter.int12BitFrom2Bytes(block, 1 + 2 * s);
        received[blockCount * SampleBlock.NUM_SAMPLES_IN_BLOCK + s] = (value - 2048) / 2048.;
      }
      blockCount++;
      progress.update("block #" + blockCount + " of " + expectedBlocks, blockCount, expectedBlocks + 1);
    }
    debug("Received " + blockCount + " blocks");
    int receivedSamples = Math.min(sampleHeader.numSamples(), blockCount * SampleBlock.NUM_SAMPLES_IN_BLOCK);
    return new ReceivedSample(sampleHeader, headerBytes, period, Arrays.copyOf(received, receivedSamples),
        blockCount, checksumErrors);
  }

  /**
   * The sampler sends the whole dump (header, all blocks and the final F7) as one single, long sysex message.
   * The midi driver hands it over in chunks of whatever size its input buffer has, not one message per block
   * (Java uses 1024 byte sysex buffers on Windows, 2048 bytes has been seen on Linux), only delivered when full or
   * when F7 arrives. This joins the chunks back into one continuous byte stream so it can be parsed block by block.
   */
  class SysexByteStream
  {
    private final Progress progress;
    final long             created = System.currentTimeMillis();
    private byte[]         chunk   = new byte[0];
    private int            pos     = 0;

    SysexByteStream(Progress progress)
    {
      this.progress = progress;
    }

    int next(long timeoutMillis) throws Exception
    {
      long start = System.currentTimeMillis();
      while (pos >= chunk.length)
      {
        if (progress.isCanceled())
        {
          throw new CancellationException("Receive cancelled");
        }
        long remaining = timeoutMillis - (System.currentTimeMillis() - start);
        if (remaining <= 0)
        {
          throw new TimeoutException("Timed out waiting for data from the sampler");
        }
        MidiMessage message = messages.poll(Math.min(remaining, 100), TimeUnit.MILLISECONDS);
        if (message instanceof SysexMessage s)
        {
          // The first chunk starts with F0. Java prepends an F7 status to continuation chunks, which is not part
          // of the stream, unless the chunk is just the terminating F7 by itself.
          boolean firstChunk = s.getStatus() == SysexMessage.SYSTEM_EXCLUSIVE;
          chunk = firstChunk || s.getData().length == 0 ? s.getMessage() : s.getData();
          pos = 0;
          debug(String.format("%6d ms: received sysex chunk, status %02x, %d bytes",
              System.currentTimeMillis() - created, s.getStatus(), chunk.length));
          trace(HexFormat.of().formatHex(chunk));
        }
      }
      return chunk[pos++] & 0xFF;
    }

    byte[] next(int count, long timeoutMillis) throws Exception
    {
      byte[] bytes = new byte[count];
      for (int i = 0; i < count; i++)
      {
        bytes[i] = (byte) next(timeoutMillis);
      }
      return bytes;
    }
  }

  private void sendSysEx(String logText, byte[] message) throws Exception
  {
    try (Receiver rcvr = midiOut.getReceiver())
    {
      debug(logText);
      SysexMessage sysMsg = new SysexMessage();
      sysMsg.setMessage(message, message.length);
      rcvr.send(sysMsg, -1);
      trace("sent sysex: ");
      trace(HexFormat.of().formatHex(message));
    }
  }

  /**
   * @return the data of the next sysex message, or null if none arrives within the timeout
   */
  private byte[] receiveSysEx(long timeoutMillis) throws InterruptedException
  {
    long start = System.currentTimeMillis();
    long remaining;
    while ((remaining = timeoutMillis - (System.currentTimeMillis() - start)) > 0)
    {
      if (messages.poll(remaining, TimeUnit.MILLISECONDS) instanceof SysexMessage s)
      {
        trace("received sysex: ");
        trace(HexFormat.of().formatHex(s.getData()));
        return s.getData();
      }
    }
    return null;
  }

  private void debug(String message)
  {
    if (debug)
    {
      System.out.println(message);
    }
  }

  private void trace(String message)
  {
    if (trace)
    {
      System.out.println(message);
    }
  }
}
