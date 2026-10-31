package axs7sds;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.function.IntFunction;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import axs7sds.SampleDump.BandWidth;
import axs7sds.SampleDump.LoopMode;
import axs7sds.SampleDump.LoopPoints;
import axs7sds.SampleDump.SampleHeader;
import axs7sds.SampleTransfer.ReceivedSample;
import axs7sds.convert.WavLoop;
import axs7sds.dsp.Resampler;
import axs7sds.midi.ByteConverter;

/**
 * Transmits test sines to the sampler and receives them back, to check the transfer and that the pitch and loop
 * survive. The sines are resampled to the nearest rate the sampler plays at, the same way as when a file is loaded,
 * and the received samples are saved and read back as wav files with their loop, as Save/Load Sample does.
 * Needs the hardware, so it is not a unit test.
 *
 * WARNING: overwrites the given sample slot on the sampler.
 *
 * <pre>
 * ./gradlew hardwareLoopback -Pdevice="XT [hw:0,0,0]" -Pslot=1 [-Prates=39555.4,48000] [-Ptests=rates|loops]
 *     [-Pdebug=true]
 * </pre>
 *
 * The sent and received samples are saved as wav files in build/loopback.
 */
public class HardwareLoopback
{
  static final int    NUM_SAMPLES = 6000;
  static final double FREQUENCY   = 440;

  public static void main(String[] args) throws Exception
  {
    if (args.length < 2 || args[1].isBlank())
    {
      System.err.println("Usage: HardwareLoopback <midi device name> <sample slot 1-6, will be overwritten> [rates]"
          + " [rates|loops]");
      System.exit(1);
    }
    int slot = Integer.parseInt(args[1]);
    if (slot < 1 || slot > 6)
    {
      throw new IllegalArgumentException("Sample slot must be 1-6");
    }
    double[] rates = args.length > 2 && !args[2].isBlank()
        ? Arrays.stream(args[2].split(",")).mapToDouble(Double::parseDouble).toArray()
        : new double[]{39555.4, 48000, 44100, 22050, 10000};
    String tests = args.length > 3 && !args[3].isBlank() ? args[3] : "rates,loops";
    boolean debug = SampleDump.booleanProp("debug");

    MidiDevice.Info in = findDevice(args[0], true);
    MidiDevice.Info out = findDevice(args[0], false);
    Path dir = Path.of("build", "loopback");
    Files.createDirectories(dir);
    System.out.println("in: " + in.getName() + ", out: " + out.getName() + ", sample slot " + slot);
    if (tests.contains("rates"))
    {
      for (double rate : rates)
      {
        loopback(in, out, slot, String.format("%.0f", rate), rate, n -> new LoopPoints(0, n - 100, LoopMode.Looping),
            dir, debug);
      }
    }
    if (tests.contains("loops"))
    {
      double rate = BandWidth.BW16.getSampleRate();
      loopback(in, out, slot, "forward", rate, n -> new LoopPoints(1000, n - 100, LoopMode.Looping), dir, debug);
      loopback(in, out, slot, "alternating", rate, n -> new LoopPoints(1000, n - 100, LoopMode.Alternating), dir,
          debug);
      loopback(in, out, slot, "oneshot", rate, n -> new LoopPoints(n - 100, n - 100, LoopMode.Looping), dir, debug);
    }
    System.exit(0); // the midi system may leave threads running
  }

  static MidiDevice.Info findDevice(String name, boolean input) throws Exception
  {
    for (MidiDevice.Info info : MidiSystem.getMidiDeviceInfo())
    {
      MidiDevice device = MidiSystem.getMidiDevice(info);
      if (device instanceof Sequencer || device instanceof Synthesizer
          || !info.getName().toLowerCase().contains(name.toLowerCase()))
      {
        continue;
      }
      if (input ? device.getMaxTransmitters() != 0 : device.getMaxReceivers() != 0)
      {
        return info;
      }
    }
    System.err.println("No midi " + (input ? "input" : "output") + " matching '" + name + "', available devices:");
    for (MidiDevice.Info info : MidiSystem.getMidiDeviceInfo())
    {
      System.err.println("  " + info.getName() + " - " + info.getDescription());
    }
    System.exit(1);
    return null;
  }

  static SampleTransfer.Progress progress(String what)
  {
    return (note, value, max) -> {
      if (value % 100 == 0 || value == max)
      {
        System.out.println("  " + what + " " + value + "/" + max);
      }
    };
  }

  /**
   * @param loopFor the loop to send, for the number of samples after resampling
   */
  static void loopback(MidiDevice.Info in, MidiDevice.Info out, int slot, String name, double rate,
      IntFunction<LoopPoints> loopFor, Path dir, boolean debug) throws Exception
  {
    System.out.printf("%n=== %s: %.1f Hz, %.0f Hz sine ===%n", name, rate, FREQUENCY);
    double[] sine = new double[NUM_SAMPLES];
    for (int i = 0; i < NUM_SAMPLES; i++)
    {
      // rising amplitude, so a copy shifted by whole periods doesn't match
      sine[i] = Math.sin(2 * Math.PI * FREQUENCY * i / rate) * (0.2 + 0.7 * i / NUM_SAMPLES);
    }
    // the same as when loading a file
    BandWidth bandWidth = BandWidth.nearest(rate);
    double tableRate = bandWidth.getSampleRate();
    double[] data = bandWidth.matches(rate) ? sine : Resampler.resample(sine, rate, tableRate);
    System.out.printf("  %s %.1f Hz (%s), %d samples%n", bandWidth.matches(rate) ? "already at" : "resampled to",
        tableRate, bandWidth.getDescription(), data.length);
    int[] encoded = SampleDump.normalizedAndScale(data, 12, 1., true);
    LoopPoints loop = loopFor.apply(data.length);
    SampleHeader sent = new SampleHeader(slot - 1, 12, (float) tableRate, data.length, loop.start(), loop.end(),
        loop.mode());

    long start = System.currentTimeMillis();
    try (SampleTransfer transfer = new SampleTransfer(in, out, debug, false))
    {
      transfer.transmit(sent, encoded, progress("transmit"));
    }
    System.out.printf("  transmitted in %.1f s%n", (System.currentTimeMillis() - start) / 1000.);
    Thread.sleep(1000);
    start = System.currentTimeMillis();
    ReceivedSample received;
    try (SampleTransfer transfer = new SampleTransfer(in, out, debug, false))
    {
      received = transfer.receive(slot - 1, progress("receive"));
    }
    System.out.printf("  received in %.1f s%n", (System.currentTimeMillis() - start) / 1000.);

    byte[] sentHeader = sent.getBytes();
    System.out.println("  header sent:     " + HexFormat.of().formatHex(sentHeader));
    System.out.println("  header received: " + HexFormat.of().formatHex(received.headerBytes()));
    System.out.printf("  period        sent %7d ns  received %7d ns%s%n", ByteConverter.intFrom3Bytes(sentHeader, 6),
        received.period(), received.validPeriod() ? "" : " (invalid)");
    System.out.printf("  total words   sent %7d     received %7d   (%d samples of data, %d blocks)%n",
        ByteConverter.intFrom3Bytes(sentHeader, 9), received.header().numSamples(), received.data().length,
        received.blocks());
    System.out.printf("  loop start    sent %7d     received %7d%n", sent.loopStart(), received.header().loopStart());
    System.out.printf("  loop end      sent %7d     received %7d%n", sent.loopEnd(), received.header().loopEnd());
    System.out.printf("  mode          sent %7d     received %7d   (%s)%n", sentHeader[18], received.headerBytes()[18],
        received.header().loopMode());
    if (received.checksumErrors() > 0)
    {
      System.out.println("  CHECKSUM ERRORS: " + received.checksumErrors());
    }

    // compare the 12 bit values, finding the offset that matches best
    int[] got = Arrays.stream(received.data()).mapToInt(d -> (int) Math.round(d * 2048 + 2048)).toArray();
    int bestOffset = 0;
    int bestMatches = -1;
    for (int offset = -200; offset <= 200; offset++)
    {
      int matches = 0;
      for (int i = 0; i < encoded.length; i++)
      {
        int j = i + offset;
        if (j >= 0 && j < got.length && got[j] == encoded[i])
        {
          matches++;
        }
      }
      if (matches > bestMatches)
      {
        bestMatches = matches;
        bestOffset = offset;
      }
    }
    int maxDiff = 0;
    int compared = 0;
    for (int i = 0; i < encoded.length; i++)
    {
      int j = i + bestOffset;
      if (j >= 0 && j < got.length)
      {
        maxDiff = Math.max(maxDiff, Math.abs(got[j] - encoded[i]));
        compared++;
      }
    }
    System.out.printf("  data: %d of %d samples identical at offset %d, max difference %d (12 bit steps)%n",
        bestMatches, compared, bestOffset, maxDiff);
    double receivedRate = received.header().sampleRate();
    double played = frequency(received.data(), receivedRate);
    System.out.printf("  received file is labelled %.1f Hz, the sine in it plays at %.2f Hz (%+.1f cents)%n",
        receivedRate, played, 1200 * Math.log(played / FREQUENCY) / Math.log(2));

    // save and load the received sample as Save/Load Sample does
    SampleHeader header = received.header();
    LoopPoints receivedLoop = new LoopPoints(header.loopStart(), header.loopEnd(), header.loopMode());
    File receivedFile = dir.resolve(name + "-received.wav").toFile();
    writeWav(receivedFile, received.data(), (float) receivedRate, receivedLoop.toWav());
    List<WavLoop.Loop> wavLoops = WavLoop.readLoops(receivedFile);
    LoopPoints loaded = LoopPoints.fromWav(wavLoops, 1., received.data().length);
    System.out.printf("  received loop %s%s, saved as %s, loaded back as %s: %s%n", receivedLoop,
        receivedLoop.oneShot() ? " (one shot)" : "", wavLoops, loaded,
        loaded.equals(receivedLoop) || (loaded.oneShot() && receivedLoop.oneShot()) ? "same" : "DIFFERENT");
    writeWav(dir.resolve(name + "-sent.wav").toFile(), sine, (float) rate,
        new LoopPoints((int) Math.round(loop.start() * rate / tableRate),
            (int) Math.round(loop.end() * rate / tableRate), loop.mode()).toWav());
  }

  /**
   * Frequency of a sine from its upward zero crossings, interpolated between samples
   */
  static double frequency(double[] data, double rate)
  {
    double first = -1;
    double last = -1;
    int periods = 0;
    for (int i = 1; i < data.length; i++)
    {
      if (data[i - 1] < 0 && data[i] >= 0)
      {
        double t = i - 1 + data[i - 1] / (data[i - 1] - data[i]);
        if (first < 0)
        {
          first = t;
        }
        else
        {
          periods++;
        }
        last = t;
      }
    }
    return periods / (last - first) * rate;
  }

  static void writeWav(File file, double[] data, float rate, List<WavLoop.Loop> loops) throws Exception
  {
    byte[] bytes = new byte[data.length * 2];
    for (int i = 0; i < data.length; i++)
    {
      int v = (int) Math.max(-32768, Math.min(32767, Math.round(data[i] * 32767)));
      bytes[2 * i] = (byte) v;
      bytes[2 * i + 1] = (byte) (v >> 8);
    }
    AudioFormat format = new AudioFormat(rate, 16, 1, true, false);
    try (AudioInputStream stream = new AudioInputStream(new ByteArrayInputStream(bytes), format, data.length))
    {
      WavLoop.write(stream, loops, file);
    }
  }
}
