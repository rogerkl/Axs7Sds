package axs7sds;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.SysexMessage;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

import axs7sds.convert.stream.ByteConvert;
import axs7sds.convert.stream.ByteConvertFactory;
import axs7sds.gui.SampleDumpPanel;
import axs7sds.gui.SampleDumpPanel.PanelOperations;
import axs7sds.gui.SampleDumpPanel.SampleFileInfo;
import axs7sds.midi.ByteConverter;

@SuppressWarnings("serial")
public class SampleDump extends JFrame implements PanelOperations
{
  private static final String              SAMPLE_HEADER      = "Sample Header";
  private static final byte[]              ACC_HEADER_BLOCK   = {(byte) 0xF0, 0x7E, 0x7F, (byte) 0xF7};
  private static final byte[]              REQUEST_SAMPLEDUMP = new byte[]{(byte) 0xF0, 0x7E, 0x0, 0x0, 0x0,
      (byte) 0xF7};
  private SampleDumpPanel                  sampleDumpPanel;
  final static int                         MAX_SAMPLES        = 32752;
  double[]                                 sampleData         = null;
  int                                      numSamples         = 0;
  private AudioFormat                      format;
  ConcurrentLinkedQueue<MidiMessageRecord> messages           = new ConcurrentLinkedQueue<>();
  private MidiDevice                       midiOut;
  AtomicBoolean                            stopThread         = new AtomicBoolean();
  boolean                                  debug              = true;
  boolean                                  trace              = true;

  enum LoopMode {
    Looping;

    byte code()
    {
      return 0;
    }
  }

  /* Standard bandwidths and the periods returned from the sampler. 
   * Assuming samplerate rounded to 2 decimals to make it possible to do the conversion both ways:   
   * Period = 1_000_000_000/samplerate (rounded to 0 decimals)
   * samplerate = samplerate/1_000_000_000 (rounded to 2 decimals)
   */
  enum BandWidth {
    BW16("16khz", 25281, 39555.4), //
    BW15("15khz", 26784, 37335.72), //
    BW14("14khz", 28377, 35239.81), //
    BW13_("13.khz", 30064, 33262.37), //
    BW13("13khz", 31852, 31395.2), //
    BW12("12khz", 33746, 29633.14), //
    BW11_("11.khz", 35753, 27969.68), //
    BW11("11khz", 37879, 26399.85), //
    BW10("10khz", 40131, 24918.39), //
    BW9_4("9.4khz", 42518, 23519.45), //
    BW8_9("8.9khz", 45046, 22199.53), //
    BW8_4("8.4khz", 47724, 20953.82), //
    BW7_9("7.9khz", 50562, 19777.7), //
    BW7_5("7.5khz", 53569, 18667.51), //
    BW7_0("7.0khz", 56754, 17619.9), //
    BW6_7("6.7khz", 60129, 16630.91), //
    BW6_3("6.3khz", 63704, 15697.6), //
    BW5_9("5.9khz", 67492, 14816.57), //
    BW5_6("5.6khz", 71506, 13984.84), //
    BW5_3("5.3khz", 75758, 13199.93), //
    BW5_0("5.0khz", 80262, 12459.2), //
    BW4_7("4.7khz", 85035, 11759.86), //
    BW4_4("4.4khz", 90091, 11099.89), //
    BW4_2("4.2khz", 95449, 10476.8), //
    BW4_0("4.0khz", 101124, 9888.85), //
    BW3_7("3.7khz", 107137, 9333.84), //
    BW3_5("3.5khz", 113508, 8809.95), //
    BW3_3("3.3khz", 120258, 8315.46), //
    BW3_1("3.1khz", 127409, 7848.74), //
    BW3_0("3.0khz", 134985, 7408.23), //
    BW2_8("2.8khz", 143011, 6992.47), //
    BW2_6("2.6khz", 151515, 6600.01), //
    BW2_5("2.5khz", 160525, 6229.56), //
    BW2_4("2.4khz", 170070, 5879.93), //
    BW2_2("2.2khz", 180183, 5549.91), //
    BW2_1("2.1khz", 190897, 5238.43), //
    BW2_0("2.0khz", 202248, 4944.42), //
    BW1_9("1.9khz", 214275, 4666.9), //
    BW1_8("1.8khz", 227016, 4404.98), //
    BW1_7("1.7khz", 240515, 4157.74), //
    BW1_6("1.6khz", 254817, 3924.38);

    String description;
    int    period;
    double sampleRate;

    private BandWidth(String description, int period, double sampleRate)
    {
      this.description = description;
      this.period = period;
      this.sampleRate = sampleRate;
    }

    public String getDescription()
    {
      return description;
    }

    public int getPeriod()
    {
      return period;
    }

    public double getSampleRate()
    {
      return sampleRate;
    }
  }

  record SampleHeader(int sampleNumber, int bits, float sampleRate, int numSamples, int loopStart, int loopEnd,
      LoopMode loopMode)
  {
    public SampleHeader
    {
      if (numSamples > MAX_SAMPLES)
        numSamples = MAX_SAMPLES;
      if (loopEnd > numSamples)
        loopEnd = numSamples;
      if (loopStart > loopEnd)
        loopStart = 0;
    }

    byte[] getBytes()
    {
      byte[] header = new byte[19];
      header[0] = (byte) 0xF0; //sysex
      header[1] = 0x7e; //non realtime identifier
      header[2] = 1; //sample dump code
      header[3] = (byte) sampleNumber; //smpnr
      header[4] = 0; //reserved
      header[5] = (byte) bits; //bits
      ByteConverter.intTo3Bytes((int) (1000000000d / sampleRate), header, 6);
      ByteConverter.intTo3Bytes(numSamples, header, 9);
      ByteConverter.intTo3Bytes(loopStart, header, 12);
      ByteConverter.intTo3Bytes(loopEnd, header, 15);
      header[18] = loopMode.code(); //looping
      return header;
    }

    /**
     * Offset value for 0 for number of bits in sampleHeader 
     * @return
     */
    public int offsetValueFor0()
    {
      return normalizedAndScale(new double[]{0.}, bits, 1., true)[0];
    }

    public int numBlocks()
    {
      int numBlocks = numSamples / SampleBlock.NUM_SAMPLES_IN_BLOCK;
      if (numBlocks > SampleBlock.NUM_BLOCKS)
        numBlocks = SampleBlock.NUM_BLOCKS;
      return numBlocks;
    }

    static public SampleHeader fromBytes(byte[] header)
    {
      return null;
      // TODO Auto-generated stub
    }
  }

  record SampleBlock(SampleHeader sampleHeader, int blockNumber, int[] sampleDataNormalizedOffsetBinary)
  {
    static public final int NUM_SAMPLES_IN_BLOCK = 60;
    static public final int NUM_BLOCKS = 546;

    byte[] getBytes(int numBlock)
    {
      byte[] data = new byte[123];
      data[0] = (byte) 0xf7;
      data[1] = (byte) (0b0000000001111111 & (blockNumber % 128));
      int sampleIndex = NUM_SAMPLES_IN_BLOCK * blockNumber;
      int offsetValueFor0 = sampleHeader.offsetValueFor0();

      for (int s = 0; s < 60; s++)
      {
        int sample = offsetValueFor0;
        if (sampleIndex < sampleHeader.numSamples())
        {
          sample = sampleDataNormalizedOffsetBinary[sampleIndex];
        }
        ByteConverter.int12BitTo2Bytes(sample, data, 2 + 2 * s);
        sampleIndex++;
      }
      byte chk = 0;
      for (int b = 2; b < 122; b++)
      {
        chk = (byte) (chk ^ data[b]);
      }
      data[122] = chk;
      return data;
    }

    static public SampleBlock fromBytes(byte[] data)
    {
      return null;
      // TODO Auto-generated stub
    }
  }

  record MidiMessageRecord(MidiMessage message, long timeStamp)
  {}

  public class SysexReceiver implements Receiver
  {
    public SysexReceiver()
    {}

    @Override
    public void send(MidiMessage message, long timeStamp)
    {
      debug("sysexreceiver: " + message.toString() + ":" + message.getLength());
      messages.add(new MidiMessageRecord(message, timeStamp));
    }

    @Override
    public void close()
    {}
  }

  class ReceiveThread extends Thread
  {
    @Override
    public void run()
    {
      MidiDevice midiIn = null;
      try
      {
        midiIn = MidiSystem.getMidiDevice(sampleDumpPanel.getSelectedMidiDeviceIn());
        midiIn.getTransmitter().setReceiver(new SysexReceiver());
        midiIn.open();

        while (!stopThread.get())
        {
          Thread.sleep(10);
        }
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
      finally
      {
        if (midiIn != null && midiIn.isOpen())
        {
          midiIn.close();
        }
      }
    }
  }

  public SampleDump()
  {
    super("Akai X7000/S700 sample dump");
    sampleDumpPanel = new SampleDumpPanel();
    setContentPane(sampleDumpPanel);
    pack();
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    sampleDumpPanel.setOperations(this);
    setVisible(true);
  }

  public static void main(String[] args)
  {
    SwingUtilities.invokeLater(() -> {
      try
      {
        Preferences prefs = Preferences.userNodeForPackage(SampleDump.class);
        String defaultLaf = "com.formdev.flatlaf.FlatDarkLaf";
        if (System.getProperty("theme") != null)
        {
          String theme = System.getProperty("theme");
          if (theme != null)
          {
            switch (theme)
            {
              case "dark" :
                prefs.put("laf", "com.formdev.flatlaf.FlatDarkLaf");
                prefs.remove("flatlafTheme");
                break;
              case "light" :
                prefs.put("laf", "com.formdev.flatlaf.FlatLightLaf");
                prefs.remove("flatlafTheme");
                break;
              case "system" :
                prefs.put("laf", UIManager.getSystemLookAndFeelClassName());
                prefs.remove("flatlafTheme");
                break;
              default :
                prefs.remove("laf");
                prefs.put("flatlafTheme", theme);
            }
          }
        }
        prefs.flush();
        if (prefs.get("flatlafTheme", null) != null)
        {
          try
          {
            Class<?> theme = Class.forName(prefs.get("flatlafTheme", null));
            Method install = theme.getDeclaredMethod("setup");
            install.invoke(null);
          }
          catch (Exception e)
          {
            System.err.println(e.getMessage());
          }
        }
        else
        {
          UIManager.setLookAndFeel(prefs.get("laf", defaultLaf));
        }
      }
      catch (Exception e)
      {
        e.printStackTrace();
        try
        {
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception e1)
        {
          e1.printStackTrace();
        }
      }
      new SampleDump();
    });
  }

  @Override
  public void exit()
  {
    System.exit(0);
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

  private boolean receiveSysEx(String logText, long start, long timeout) throws Exception
  {
    boolean ok = false;
    while (!ok && (System.currentTimeMillis() - start) < timeout)
    {
      MidiMessageRecord poll = messages.poll();
      if (poll != null)
      {
        debug(logText + " " + poll.message().getLength());
        if (poll.message instanceof SysexMessage s)
        {
          byte[] data = s.getData();
          trace("received sysex: ");
          trace(HexFormat.of().formatHex(data));
          if (data.length > 18 && SAMPLE_HEADER.equals(logText))
          {
            //          0 0F0h B System-exclusive code
            //          1 07Eh B Common non-realtime identifier
            //          2 1 B SD, Sample dump code
            //          3 N B LS, Sample number. Incoming sample will replace X7000's sample numbered N+1 where N is 0 to 5. if N>5, then the sample being edited will be replaced.
            //          4 0 B Reserved (must be 0).
            //          5 8-16 B Bits per sample word. X7000 transmits 12, but can accept 8 to 14.
            //          6-8 0-127 TB Sampling period in nS. X7000 accepts 15259 to 500000. X7000 assumes the nominal pitch of samples is C3 (MIDI 60) unless a subsequent set of sample parameters specifies it otherwise.
            //          9-11 0-127 TB Total words in sample. X7000 accepts 200 to 32752.
            //          12-14 0-127 TB Loop start point. If it is >= Total word - 5, assume non-looping.
            //          15-17 0-127 TB Loop end. X7000 takes this as sample end point. It takes the start point as 0, unless subsequent set of sample parameters specify it otherwise.
            //          18 0-1 B Mode, 0=looping, 1=alternating (One shot, if loop length < 5).
            //
            debug(data[0]);
            debug(data[1]);
            debug(data[2]);
            debug(data[3]);
            debug(data[4]);
            debug(data[5]);
            debug(ByteConverter.intFrom3Bytes(data, 6));
            debug(ByteConverter.intFrom3Bytes(data, 9));
            debug(ByteConverter.intFrom3Bytes(data, 12));
            debug(ByteConverter.intFrom3Bytes(data, 15));
            debug(data[18]);
          }
          ok = true;
        }
      }
    }
    return ok;
  }

  private void debug(double val)
  {
    if (debug)
      System.out.println(val);
  }

  private void debug(byte val)
  {
    if (debug)
      System.out.println(val);
  }

  private void debug(String message)
  {
    if (debug)
      System.out.println(message);
  }

  private void trace(double val)
  {
    if (trace)
      System.out.println(val);
  }

  private void trace(byte val)
  {
    if (trace)
      System.out.println(val);
  }

  private void trace(String message)
  {
    if (trace)
      System.out.println(message);
  }

  @Override
  public void receive()
  {
    try
    {
      stopThread.set(false);
      new ReceiveThread().start();
      midiOut = MidiSystem.getMidiDevice(sampleDumpPanel.getSelectedMidiDeviceOut());
      midiOut.open();

      REQUEST_SAMPLEDUMP[3] = (byte) sampleDumpPanel.getSampleNumber();

      sendSysEx("Requesting sample dump", REQUEST_SAMPLEDUMP);
      if (receiveSysEx(SAMPLE_HEADER, System.currentTimeMillis(), 20_000))
      {
        Thread.sleep(1);
        sendSysEx("Acc", ACC_HEADER_BLOCK);
        int count = 0;
        long start = System.currentTimeMillis();
        boolean ok = false;
        do
        {
          ok = receiveSysEx("Sample Data", start, 8_000);
          if (ok)
          {
            Thread.sleep(1);
            sendSysEx("Acc", ACC_HEADER_BLOCK);
            count++;
          }
        }
        while (count < 546 && ok);
        if (ok)
        {
          receiveSysEx("End transfer", System.currentTimeMillis(), 8_000);
        }
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
      JOptionPane.showMessageDialog(this, e.getMessage());
    }
    finally
    {
      if (midiOut != null && midiOut.isOpen())
      {
        midiOut.close();
      }
      midiOut = null;
      stopThread.set(true);
    }
  }

  @Override
  public void transmit()
  {
    ProgressMonitor progressMonitor = new ProgressMonitor(sampleDumpPanel, "Sending data", "Start", 0, 547);
    new Thread(() -> _transmit(progressMonitor)).start();
  }

  /*
  BYTE RANGE TYPE DESCRIPTION
  0 0F0h B System-exclusive code
  1 07Eh B Common non-realtime identifier
  2 1 B SD, Sample dump code
  3 N B LS, Sample number. Incoming sample will replace X7000's sample numbered N+1 where N is 0 to 5. if N>5, then the sample being edited will be replaced.
  4 0 B Reserved (must be 0).
  5 8-16 B Bits per sample word. X7000 transmits 12, but can accept 8 to 14.
  6-8 0-127 TB Sampling period in nS. X7000 accepts 15259 to 500000. X7000 assumes the nominal pitch of samples is C3 (MIDI 60) unless a subsequent set of sample parameters specifies it otherwise.
  9-11 0-127 TB Total words in sample. X7000 accepts 200 to 32752.
  12-14 0-127 TB Loop start point. If it is >= Total word - 5, assume non-looping.
  15-17 0-127 TB Loop end. X7000 takes this as sample end point. It takes the start point as 0, unless subsequent set of sample parameters specify it otherwise.
  18 0-1 B Mode, 0=looping, 1=alternating (One shot, if loop length < 5).
  
  Next follows a variable number of sample blocks. Each has the
  same format:
  0 0-127 B Block number LSByte.
  1,2 0-4095 SW Sample data.
  3-120 0-4095 SW 59 more sample data points.
  121 0-127 B Checksum, XOR of previous 120 bytes.
  
  A final byte follows the last block:
  ?? 0F7h B End-Of-Exclusive.
  */
  public void _transmit(ProgressMonitor progressMonitor)
  {
    try
    {
      stopThread.set(false);
      new ReceiveThread().start();
      midiOut = MidiSystem.getMidiDevice(sampleDumpPanel.getSelectedMidiDeviceOut());
      midiOut.open();

      int bits = 12;
      SampleHeader sampleHeader = getSampleHeader(bits);
      sendSysEx("Sending sample", sampleHeader.getBytes());
      double[] sampleDataCopy = getSampleDataWithXFade(sampleHeader);

      double max = 0.;
      double normalize = sampleDumpPanel.getNormalizeValue(); //normalize to get highest snr as possible for 12 bits
      for (int i = 0; i < sampleHeader.numSamples(); i++)
      {
        double sample = Math.abs(sampleDataCopy[i]);
        if (sample > max)
          max = sample;
      }
      if (max > 0 && sampleDumpPanel.normalize())
      {
        normalize = normalize * 1. / max;
      }
      else
      {
        normalize = 1.;
      }
      System.out.println(max);
      System.out.println(normalize);
      int[] sampleDataNormalizedAndDithered = normalizedAndScale(sampleDataCopy, bits, normalize, true);

      for (int i = 0; i < sampleHeader.numBlocks(); i++)
      {
        int fi = i;
        SwingUtilities.invokeLater(() -> {
          progressMonitor.setNote("packet #" + fi);
          progressMonitor.setProgress(fi + 1);
        });
        byte[] data = new SampleBlock(sampleHeader, i, sampleDataNormalizedAndDithered).getBytes(i);
        sendSysEx("Sending block# " + i, data);
        if (!receiveSysEx("Wait Ack", System.currentTimeMillis(), 1000))
        {
          throw new Exception("no ack received!");
        }
      }
      sendSysEx("End", new byte[]{(byte) 0xf7});
      SwingUtilities.invokeLater(() -> {
        progressMonitor.setNote("Finished");
        progressMonitor.setProgress(547);
      });
    }
    catch (Exception e)
    {
      e.printStackTrace();
      JOptionPane.showMessageDialog(this, e.getMessage());
    }
    finally
    {
      if (midiOut != null && midiOut.isOpen())
      {
        midiOut.close();
      }
      midiOut = null;
      stopThread.set(true);
    }
  }

  private SampleHeader getSampleHeader(int bits)
  {
    SampleHeader sampleHeader = new SampleHeader(sampleDumpPanel.getSampleNumber(), bits, format.getSampleRate(),
        sampleDumpPanel.getNumSamples(), sampleDumpPanel.getLoopStart(), sampleDumpPanel.getLoopEnd(),
        LoopMode.Looping);
    return sampleHeader;
  }

  private double[] getSampleDataWithXFade(SampleHeader sampleHeader)
  {
    double[] sampleDataCopy = Arrays.copyOf(sampleData, sampleHeader.numSamples());
    int xfadeLength = sampleDumpPanel.getXfadeLength();
    if (xfadeLength > 0)
    {
      int xfStartIndex = sampleHeader.loopStart - xfadeLength;//1000-5000 = 5000
      int xfEndIndex = sampleHeader.loopEnd - xfadeLength;//32752-5000 = 27752
      for (int i = 0; i < xfadeLength; i++)
      {
        double valEnd = sampleDataCopy[xfEndIndex + i];//27752
        double valStart = sampleDataCopy[xfStartIndex + i];//5000
        double d = ((double) (i + 1)) / ((double) (xfadeLength));//0=0 ,4999=1
        sampleDataCopy[xfEndIndex + i] = valEnd * (1. - d) + valStart * d;
      }
    }
    return sampleDataCopy;
  }

  private static int[] normalizedAndScale(double[] data, int bits, double normalize, boolean unsigned)
  {
    int[] outData = new int[data.length];
    double ranged2 = Math.pow(2.0, bits - 1);
    double mult = ranged2 - 1;

    for (int i = 0; i < data.length; i++)
    {
      double in = data[i] * normalize * mult;

      if (unsigned)
        in = in + ranged2;

      outData[i] = (int) in;
    }
    return outData;
  }

  @Override
  public void saveSample()
  {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setFileFilter(new FileNameExtensionFilter("Audio file",
        AudioFileFormat.Type.WAVE.getExtension(),
        AudioFileFormat.Type.AU.getExtension(),
        AudioFileFormat.Type.AIFF.getExtension(),
        AudioFileFormat.Type.AIFC.getExtension(),
        AudioFileFormat.Type.SND.getExtension()));
    if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
    {
      try
      {
        SampleHeader sampleHeader = getSampleHeader(12);
        double[] sampleDataCopy = getSampleDataWithXFade(sampleHeader);

        AudioFormat outformat = new AudioFormat(format.getEncoding(), format.getSampleRate(),
            format.getSampleSizeInBits(), 1, format.getFrameSize() / format.getChannels(), format.getSampleRate(),
            format.isBigEndian());

        ByteConvert converter = ByteConvertFactory.createConverter(outformat);
        numSamples = sampleDumpPanel.getNumSamples();
        File selectedFile = fileChooser.getSelectedFile();
        byte[] convBuffer = new byte[outformat.getFrameSize() * numSamples];
        converter.toBytes(sampleDataCopy, convBuffer, numSamples, 1, 0);

        ByteArrayInputStream bais = new ByteArrayInputStream(convBuffer);

        try (AudioInputStream audioInputStream = new AudioInputStream(bais, outformat, numSamples))
        {
          AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, selectedFile);
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
  }

  @Override
  public void loadSample()
  {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setFileFilter(new FileNameExtensionFilter("WAV file", "wav"));
    if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
    {
      File selectedFile = fileChooser.getSelectedFile();
      try (AudioInputStream inputStream = AudioSystem.getAudioInputStream(selectedFile))
      {
        format = inputStream.getFormat();

        byte[] bytes = new byte[format.getFrameSize() * MAX_SAMPLES];
        int read = inputStream.read(bytes, 0, format.getFrameSize() * MAX_SAMPLES);
        numSamples = read / format.getFrameSize();

        ByteConvert converter = ByteConvertFactory.createConverter(format);
        double[][] convBuffer = new double[format.getChannels()][numSamples];
        converter.fromBytes(bytes, convBuffer, numSamples);
        sampleData = new double[numSamples];
        for (int i = 0; i < numSamples; i++)
        {
          sampleData[i] = convBuffer[0][i];
        }

        sampleDumpPanel.setSampleFile(new SampleFileInfo(selectedFile, format.getChannels(), format.getSampleRate(),
            format.getSampleSizeInBits(), numSamples, format.getEncoding().toString()));
        sampleDumpPanel.setNumSamples(numSamples);
        sampleDumpPanel.setLoopStart(0);
        sampleDumpPanel.setLoopEnd(numSamples);
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
  }
}
