package axs7sds;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;

import javax.sound.midi.SysexMessage;

import org.junit.Test;

import axs7sds.SampleDump.LoopMode;
import axs7sds.SampleDump.SampleBlock;
import axs7sds.SampleDump.SampleHeader;
import axs7sds.SampleTransfer.ReceivedSample;

public class SampleTransferTest
{
  private static final SampleTransfer.Progress NO_PROGRESS = (note, value, max) -> {};

  private final SampleHeader header  = new SampleHeader(2, 12, 39555.4f, 1000, 100, 900, LoopMode.Looping);
  private final int[]        encoded = SampleDump.normalizedAndScale(sine(1000), 12, 1., true);

  private static double[] sine(int n)
  {
    double[] data = new double[n];
    for (int i = 0; i < n; i++)
    {
      data[i] = Math.sin(i * 0.05) * 0.9;
    }
    return data;
  }

  /**
   * The bytes of a dump as one continuous sysex message, encoded the same way as when transmitting
   */
  private byte[] dump(boolean headerEndsWithF7)
  {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    stream.writeBytes(header.getBytes());
    if (headerEndsWithF7)
    {
      stream.write(0xF7);
    }
    for (int b = 0; b < header.numBlocks(); b++)
    {
      byte[] block = new SampleBlock(header, b, encoded).getBytes(b);
      stream.write(block, 1, block.length - 1); // without the F7 status Java needs to send a continuation
    }
    stream.write(0xF7);
    return stream.toByteArray();
  }

  /**
   * Splits the stream into messages the way Java delivers a long sysex message in chunks: continuation chunks get
   * an F7 status prepended, unless they start with F0 or F7.
   */
  private static void deliver(SampleTransfer transfer, byte[] stream, int chunkSize) throws Exception
  {
    for (int off = 0; off < stream.length; off += chunkSize)
    {
      int len = Math.min(chunkSize, stream.length - off);
      int first = stream[off] & 0xFF;
      boolean continuation = first != 0xF0 && first != 0xF7;
      byte[] message = new byte[len + (continuation ? 1 : 0)];
      message[0] = (byte) 0xF7;
      System.arraycopy(stream, off, message, continuation ? 1 : 0, len);
      transfer.messages.add(new SysexMessage(message, message.length));
    }
  }

  private void assertReceived(ReceivedSample received)
  {
    assertEquals(header.nunSamplesWithLoopFix(), received.header().numSamples());
    assertEquals(header.loopStart(), received.header().loopStart());
    assertEquals(header.loopEnd(), received.header().loopEnd());
    assertEquals(header.numBlocks(), received.blocks());
    assertEquals(0, received.checksumErrors());
    for (int i = 0; i < received.data().length; i++)
    {
      int expected = i < encoded.length ? encoded[i] : header.offsetValueFor0();
      assertEquals("sample " + i, (expected - 2048) / 2048., received.data()[i], 1e-9);
    }
  }

  @Test
  public void receivesDumpInAnyChunkSize() throws Exception
  {
    for (int chunkSize : new int[]{1, 7, 19, 122, 1024, 2048, 100_000})
    {
      SampleTransfer transfer = new SampleTransfer(false, false);
      deliver(transfer, dump(false), chunkSize);
      assertReceived(transfer.readDump(transfer.new SysexByteStream(NO_PROGRESS), NO_PROGRESS));
    }
  }

  @Test
  public void skipsF7AfterHeader() throws Exception
  {
    SampleTransfer transfer = new SampleTransfer(false, false);
    deliver(transfer, dump(true), 1024);
    assertReceived(transfer.readDump(transfer.new SysexByteStream(NO_PROGRESS), NO_PROGRESS));
  }

  @Test
  public void fallsBackToBw16SampleRateForInvalidPeriod()
  {
    // header received from an X7000, with "din" in the sampling period
    byte[] bytes = java.util.HexFormat.of().parseHex("f07e0100000c64696e0c7f010000000c7f0100");
    SampleHeader received = SampleHeader.fromBytes(bytes);
    assertEquals(SampleDump.BandWidth.BW16.getSampleRate(), received.sampleRate(), 0.01);
    assertEquals(32652, received.numSamples());
  }
}
