package axs7sds;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.List;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import axs7sds.SampleDump.LoopMode;
import axs7sds.SampleDump.LoopPoints;
import axs7sds.SampleDump.SampleHeader;
import axs7sds.convert.WavLoop;
import axs7sds.convert.WavLoop.Loop;

public class LoopTest
{
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  private static AudioInputStream audio(int frames, int bits)
  {
    AudioFormat format = new AudioFormat(39555.4f, bits, 1, bits > 8, false);
    return new AudioInputStream(new ByteArrayInputStream(new byte[frames * bits / 8]), format, frames);
  }

  @Test
  public void headerKeepsLoopMode()
  {
    SampleHeader sent = new SampleHeader(0, 12, 39555.4f, 1000, 100, 900, LoopMode.Alternating);
    byte[] bytes = sent.getBytes();
    assertEquals(1, bytes[18]);
    assertEquals(LoopMode.Alternating, SampleHeader.fromBytes(bytes).loopMode());
    bytes[18] = 0;
    assertEquals(LoopMode.Looping, SampleHeader.fromBytes(bytes).loopMode());
  }

  @Test
  public void wavLoopsRoundTrip() throws Exception
  {
    for (List<Loop> loops : List.of(List.of(new Loop(100, 900, false)), List.of(new Loop(1, 2, true)),
        List.<Loop> of()))
    {
      File file = folder.newFile();
      WavLoop.write(audio(1000, 16), loops, file);
      assertEquals(loops, WavLoop.readLoops(file));
      // the audio is still readable
      try (AudioInputStream in = AudioSystem.getAudioInputStream(file))
      {
        assertEquals(1000, in.getFrameLength());
      }
    }
  }

  @Test
  public void wavLoopAfterOddLengthData() throws Exception
  {
    File file = folder.newFile();
    WavLoop.write(audio(3, 8), List.of(new Loop(0, 2, false)), file);
    assertEquals(List.of(new Loop(0, 2, false)), WavLoop.readLoops(file));
    assertEquals(0, file.length() % 2);
  }

  @Test
  public void wavWithoutSmplChunkHasNoLoopInformation() throws Exception
  {
    File file = folder.newFile();
    AudioSystem.write(audio(1000, 16), AudioFileFormat.Type.WAVE, file);
    assertNull(WavLoop.readLoops(file));
  }

  @Test
  public void loopPointsFromWav()
  {
    // no loop information: as before, loop to 100 samples before the end
    assertEquals(new LoopPoints(0, 900, LoopMode.Looping), LoopPoints.fromWav(null, 1., 1000));
    // no loop: one shot
    LoopPoints oneShot = LoopPoints.fromWav(List.of(), 1., 1000);
    assertTrue(oneShot.oneShot());
    assertEquals(List.of(), oneShot.toWav());
    // resampled from 48000 Hz
    assertEquals(new LoopPoints(824, 4120, LoopMode.Alternating),
        LoopPoints.fromWav(List.of(new Loop(1000, 5000, true)), 39555.4 / 48000, 32752));
    // truncated sample
    assertEquals(new LoopPoints(500, 600, LoopMode.Looping),
        LoopPoints.fromWav(List.of(new Loop(500, 5000, false)), 1., 600));
    assertEquals(List.of(new Loop(500, 600, false)), new LoopPoints(500, 600, LoopMode.Looping).toWav());
  }
}
