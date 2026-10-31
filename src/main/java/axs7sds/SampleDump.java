package axs7sds;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;

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

import axs7sds.SampleTransfer.ReceivedSample;
import axs7sds.convert.stream.ByteConvert;
import axs7sds.convert.stream.ByteConvertFactory;
import axs7sds.dsp.MammothFftLoop;
import axs7sds.dsp.Resampler;
import axs7sds.gui.SampleDumpPanel;
import axs7sds.gui.SampleDumpPanel.PanelOperations;
import axs7sds.gui.SampleDumpPanel.SampleFileInfo;
import axs7sds.gui.SampleDumpPanel.SamplePanelParameters;
import axs7sds.midi.ByteConverter;

@SuppressWarnings("serial")
public class SampleDump extends JFrame implements PanelOperations
{
  private SampleDumpPanel                  sampleDumpPanel;
  public final static int                  MAX_SAMPLES        = 32752;
  double[]                                 sampleData         = null;
  int                                      numSamples         = 0;
  private AudioFormat                      format;
  /** set while a transmit or receive is running, so a second one is not started on top of it */
  AtomicBoolean                            busy               = new AtomicBoolean();
  boolean                                  debug              = booleanProp("debug");
  boolean                                  trace              = booleanProp("trace");
  private SamplePanelParameters            samplePanelParameters;

  static boolean booleanProp(String property)
  {
    return "true".equalsIgnoreCase(System.getProperty(property, "false"));
  }

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

    /**
     * The sampler plays samples at these rates, a semitone apart. It rounds other rates down to one of them and
     * keeps the rest as fine tune, which it applies in the wrong direction and leaves out of a sample dump, so
     * samples are resampled to one of these rates before they are sent.
     *
     * @return the rate nearest in pitch, BW16 for rates above it
     */
    static BandWidth nearest(double sampleRate)
    {
      BandWidth nearest = BW16;
      for (BandWidth bandWidth : values())
      {
        if (Math.abs(cents(sampleRate, bandWidth.sampleRate)) < Math.abs(cents(sampleRate, nearest.sampleRate)))
        {
          nearest = bandWidth;
        }
      }
      return nearest;
    }

    /**
     * @return true if the sample rate is this rate, within the precision of a wav file (whole Hz)
     */
    boolean matches(double sampleRate)
    {
      return Math.abs(cents(sampleRate, this.sampleRate)) < 1;
    }

    static double cents(double sampleRate, double reference)
    {
      return 1200 * Math.log(sampleRate / reference) / Math.log(2);
    }
  }

  record SampleHeader(int sampleNumber, int bits, float sampleRate, int numSamples, int loopStart, int loopEnd,
      LoopMode loopMode)
  {
    public SampleHeader
    {
      if (numSamples > MAX_SAMPLES)
      {
        numSamples = MAX_SAMPLES;
      }
      if (loopEnd > numSamples)
      {
        loopEnd = numSamples;
      }
      if (loopStart > loopEnd)
      {
        loopStart = 0;
      }
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
      ByteConverter.intTo3Bytes(period(), header, 6);
      ByteConverter.intTo3Bytes(nunSamplesWithLoopFix(), header, 9);//loop trouble
      ByteConverter.intTo3Bytes(loopStart, header, 12);
      ByteConverter.intTo3Bytes(loopEnd, header, 15);
      header[18] = loopMode.code(); //looping
      return header;
    }

    /**
     * @return the sampling period in ns, the exact table period if the rate is one of the sampler's
     */
    public int period()
    {
      BandWidth bandWidth = BandWidth.nearest(sampleRate);
      return bandWidth.matches(sampleRate) ? bandWidth.getPeriod() : (int) (1000000000d / sampleRate);
    }

    public int nunSamplesWithLoopFix()
    {
      return Math.max(Math.min(numSamples + 100, MAX_SAMPLES), Math.min(numSamples + 200, MAX_SAMPLES));
    }

    /**
     * Offset value for 0 for number of bits in sampleHeader
     * @return
     */
    /**
     * The X7000 accepts periods from 15259 to 500000 ns, but has been seen sending garbage in this field
     */
    static boolean validPeriod(int period)
    {
      return period >= 15259 && period <= 500000;
    }

    public int offsetValueFor0()
    {
      return normalizedAndScale(new double[]{0.}, bits, 1., true)[0];
    }

    public int numBlocks()
    {
      int numBlocks = nunSamplesWithLoopFix() / SampleBlock.NUM_SAMPLES_IN_BLOCK;
      if (numBlocks > SampleBlock.NUM_BLOCKS)
      {
        numBlocks = SampleBlock.NUM_BLOCKS;
      }
      return numBlocks;
    }

    /**
     * @param header the header bytes, including the leading F0
     */
    static public SampleHeader fromBytes(byte[] header)
    {
      int period = ByteConverter.intFrom3Bytes(header, 6);
      return new SampleHeader(header[3], header[5],
          validPeriod(period) ? (float) (1_000_000_000d / period) : (float) BandWidth.BW16.getSampleRate(),
          ByteConverter.intFrom3Bytes(header, 9),
          ByteConverter.intFrom3Bytes(header, 12),
          ByteConverter.intFrom3Bytes(header, 15),
          LoopMode.Looping);
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

  /**
   * Shows the progress of a transfer in a progress monitor
   */
  private static SampleTransfer.Progress progressFor(ProgressMonitor progressMonitor)
  {
    return new SampleTransfer.Progress()
    {
      @Override
      public void update(String note, int value, int max)
      {
        SwingUtilities.invokeLater(() -> {
          progressMonitor.setMaximum(max);
          progressMonitor.setNote(note);
          progressMonitor.setProgress(value);
        });
      }

      @Override
      public boolean isCanceled()
      {
        return progressMonitor.isCanceled();
      }
    };
  }

  @Override
  public void receive()
  {
    if (!busy.compareAndSet(false, true))
    {
      return;
    }
    try
    {
      samplePanelParameters = sampleDumpPanel.getParameters();
      ProgressMonitor progressMonitor = new ProgressMonitor(sampleDumpPanel, "Receiving data",
          "Waiting for the sampler", 0, SampleBlock.NUM_BLOCKS + 1);
      progressMonitor.setMillisToDecideToPopup(0);
      progressMonitor.setMillisToPopup(0);
      progressMonitor.setProgress(0);
      new Thread(() -> _receive(progressMonitor)).start();
    }
    catch (RuntimeException e)
    {
      busy.set(false);
      throw e;
    }
  }

  public void _receive(ProgressMonitor progressMonitor)
  {
    try (SampleTransfer transfer = new SampleTransfer(sampleDumpPanel.getSelectedMidiDeviceIn(),
        sampleDumpPanel.getSelectedMidiDeviceOut(), debug, trace))
    {
      ReceivedSample received = transfer.receive(samplePanelParameters.sampleNumber(), progressFor(progressMonitor));
      SampleHeader sampleHeader = received.header();
      int receivedSamples = received.data().length;
      AudioFormat receivedFormat = new AudioFormat(sampleHeader.sampleRate(), 16, 1, true, false);
      SwingUtilities.invokeLater(() -> {
        progressMonitor.close();
        sampleData = received.data();
        numSamples = receivedSamples;
        format = receivedFormat;
        sampleDumpPanel.setSampleFile(new SampleFileInfo(null, 1, sampleHeader.sampleRate(), sampleHeader.bits(),
            receivedSamples, "received sample #" + (sampleHeader.sampleNumber() + 1), sampleHeader.sampleRate()));
        sampleDumpPanel.setNumSamples(receivedSamples);
        sampleDumpPanel.setLoopStart(Math.min(sampleHeader.loopStart(), receivedSamples));
        sampleDumpPanel.setLoopEnd(Math.min(sampleHeader.loopEnd(), receivedSamples));
        if (receivedSamples < sampleHeader.numSamples() || received.checksumErrors() > 0 || !received.validPeriod())
        {
          JOptionPane.showMessageDialog(this, "Received " + receivedSamples + " of " + sampleHeader.numSamples()
              + " samples, " + received.checksumErrors() + " blocks with checksum errors"
              + (received.validPeriod() ? ""
                  : "\nInvalid sample period in header (" + received.period() + " ns), assuming "
                      + BandWidth.BW16.getSampleRate() + " Hz"));
        }
      });
    }
    catch (CancellationException e)
    {
      // cancelled by the user
    }
    catch (Exception e)
    {
      e.printStackTrace();
      SwingUtilities.invokeLater(() -> {
        progressMonitor.close();
        JOptionPane.showMessageDialog(this, e.getMessage());
      });
    }
    finally
    {
      busy.set(false);
    }
  }

  @Override
  public void transmit()
  {
    if (!busy.compareAndSet(false, true))
    {
      return;
    }
    try
    {
      samplePanelParameters = sampleDumpPanel.getParameters();
      ProgressMonitor progressMonitor = new ProgressMonitor(sampleDumpPanel, "Sending data", "Start", 0, 547);
      new Thread(() -> _transmit(progressMonitor)).start();
    }
    catch (RuntimeException e)
    {
      busy.set(false);
      throw e;
    }
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
    try (SampleTransfer transfer = new SampleTransfer(sampleDumpPanel.getSelectedMidiDeviceIn(),
        sampleDumpPanel.getSelectedMidiDeviceOut(), debug, trace))
    {
      int bits = 12;
      SampleHeader sampleHeader = getSampleHeader(bits);
      double[] sampleDataCopy = getSampleDataWithXFade(sampleHeader);

      double max = 0.;
      double normalize = samplePanelParameters.normalizeValue(); //normalize to get highest snr as possible for 12 bits
      for (int i = 0; i < sampleHeader.numSamples(); i++)
      {
        double sample = Math.abs(sampleDataCopy[i]);
        if (sample > max)
        {
          max = sample;
        }
      }
      if (max > 0 && samplePanelParameters.normalize())
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
      transfer.transmit(sampleHeader, sampleDataNormalizedAndDithered, progressFor(progressMonitor));
    }
    catch (Exception e)
    {
      e.printStackTrace();
      SwingUtilities.invokeLater(() -> {
        progressMonitor.close();
        JOptionPane.showMessageDialog(this, e.getMessage());
      });
    }
    finally
    {
      busy.set(false);
    }
  }

  private SampleHeader getSampleHeader(int bits)
  {
    SampleHeader sampleHeader = new SampleHeader(samplePanelParameters.sampleNumber(), bits, format.getSampleRate(),
        samplePanelParameters.numSamples(), samplePanelParameters.loopStart(),
        samplePanelParameters.loopEnd(),
        LoopMode.Looping);
    return sampleHeader;
  }

  private double[] getSampleDataWithXFade(SampleHeader sampleHeader)
  {
    double[] sampleDataCopy;
    if (samplePanelParameters.fftLoopGenerate())
    {
      sampleDataCopy = MammothFftLoop.process(sampleData,
          samplePanelParameters.numSamples() - samplePanelParameters.fadeIn(), samplePanelParameters.fadeIn(),
          samplePanelParameters.phaseMult(), samplePanelParameters.binPower());
    }
    else
    {
      sampleDataCopy = Arrays.copyOf(sampleData, sampleHeader.numSamples());
      int xfadeLength = samplePanelParameters.xfadeLength();
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
    }
    return sampleDataCopy;
  }

  static int[] normalizedAndScale(double[] data, int bits, double normalize, boolean unsigned)
  {
    int[] outData = new int[data.length];
    double ranged2 = Math.pow(2.0, bits - 1);
    double mult = ranged2 - 1;

    for (int i = 0; i < data.length; i++)
    {
      double in = data[i] * normalize * mult;

      if (unsigned)
      {
        in = in + ranged2;
      }

      outData[i] = (int) in;
    }
    return outData;
  }

  @Override
  public void saveSample()
  {
    samplePanelParameters = sampleDumpPanel.getParameters();
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
        numSamples = samplePanelParameters.numSamples();
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
        AudioFormat fileFormat = inputStream.getFormat();
        BandWidth bandWidth = BandWidth.nearest(fileFormat.getSampleRate());
        boolean resample = !bandWidth.matches(fileFormat.getSampleRate());

        // read enough to fill the sampler after resampling, plus a little for the resampling filter
        int frames = (int) Math.ceil(MAX_SAMPLES * fileFormat.getSampleRate() / bandWidth.getSampleRate()) + 200;
        byte[] bytes = new byte[fileFormat.getFrameSize() * frames];
        int read = 0;
        int n;
        while (read < bytes.length && (n = inputStream.read(bytes, read, bytes.length - read)) > 0)
        {
          read += n;
        }
        int fileSamples = read / fileFormat.getFrameSize();

        ByteConvert converter = ByteConvertFactory.createConverter(fileFormat);
        double[][] convBuffer = new double[fileFormat.getChannels()][fileSamples];
        converter.fromBytes(bytes, convBuffer, fileSamples);
        double[] data = convBuffer[0];
        if (resample)
        {
          data = Resampler.resample(data, fileFormat.getSampleRate(), bandWidth.getSampleRate());
        }
        sampleData = Arrays.copyOf(data, Math.min(data.length, MAX_SAMPLES));
        numSamples = sampleData.length;
        float sampleRate = (float) bandWidth.getSampleRate();
        format = new AudioFormat(fileFormat.getEncoding(), sampleRate, fileFormat.getSampleSizeInBits(),
            fileFormat.getChannels(), fileFormat.getFrameSize(), sampleRate, fileFormat.isBigEndian());

        sampleDumpPanel.setSampleFile(new SampleFileInfo(selectedFile, fileFormat.getChannels(), sampleRate,
            fileFormat.getSampleSizeInBits(), numSamples, fileFormat.getEncoding().toString(),
            resample ? fileFormat.getSampleRate() : sampleRate));
        sampleDumpPanel.setNumSamples(numSamples);
        sampleDumpPanel.setLoopStart(0);
        sampleDumpPanel.setLoopEnd(numSamples - 100);
        sampleDumpPanel.checkFftLoop();
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
  }
}
