package axs7sds.convert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Writes the loops of a wav file as regions in an Audacity label file, since Audacity ignores the loop information
 * in wav files. Import it in Audacity with File &gt; Import &gt; Labels.
 */
public class AudacityLabels
{
  /**
   * @return the label file for a wav file: the same name with a .txt extension
   */
  public static File labelFileFor(File wav)
  {
    String name = wav.getName();
    int dot = name.lastIndexOf('.');
    return new File(wav.getAbsoluteFile().getParentFile(), (dot > 0 ? name.substring(0, dot) : name) + ".txt");
  }

  /**
   * Writes the loops as regions in seconds, at the sample rate in the wav file header, so they start and end at the
   * loop's samples in Audacity
   *
   * @return the label file, or null if there are no loops (one shot) or the wav file itself ends in .txt
   */
  public static File write(File wav, List<WavLoop.Loop> loops) throws IOException, UnsupportedAudioFileException
  {
    File labels = labelFileFor(wav);
    if (loops.isEmpty() || labels.equals(wav.getAbsoluteFile()))
    {
      return null;
    }
    double rate = AudioSystem.getAudioFileFormat(wav).getFormat().getSampleRate();
    StringBuilder text = new StringBuilder();
    for (WavLoop.Loop loop : loops)
    {
      text.append(String.format(Locale.ROOT, "%.6f\t%.6f\tloop %s\n", loop.start() / rate, loop.end() / rate,
          loop.alternating() ? "alternating" : "forward"));
    }
    Files.writeString(labels.toPath(), text);
    return labels;
  }
}
