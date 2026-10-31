package axs7sds.convert;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * Reads and writes the loops of a wav file, kept in a "smpl" chunk, which Java's audio system ignores.
 */
public class WavLoop
{
  private static final int SMPL_HEADER_SIZE = 36;
  private static final int SMPL_LOOP_SIZE   = 24;

  /**
   * @param start first sample in the loop
   * @param end sample after the last sample in the loop (in the smpl chunk it is the last sample in the loop)
   * @param alternating true for a back and forth loop, false for a forward loop
   */
  public record Loop(int start, int end, boolean alternating)
  {}

  /**
   * Writes a wav file with a smpl chunk
   *
   * @param loops the loops, empty for a sample without a loop (one shot)
   */
  public static void write(AudioInputStream audio, List<Loop> loops, File file) throws IOException
  {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    AudioSystem.write(audio, AudioFileFormat.Type.WAVE, out);
    if (out.size() % 2 != 0)
    {
      out.write(0); // chunks start at even offsets
    }
    out.writeBytes(smplChunk(loops, audio.getFormat().getSampleRate()));
    byte[] wav = out.toByteArray();
    ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN).putInt(4, wav.length - 8); // RIFF chunk size
    Files.write(file.toPath(), wav);
  }

  static byte[] smplChunk(List<Loop> loops, float sampleRate)
  {
    int size = SMPL_HEADER_SIZE + SMPL_LOOP_SIZE * loops.size();
    ByteBuffer chunk = ByteBuffer.allocate(8 + size).order(ByteOrder.LITTLE_ENDIAN);
    chunk.put("smpl".getBytes(StandardCharsets.US_ASCII)).putInt(size);
    chunk.putInt(0).putInt(0); // manufacturer, product
    chunk.putInt((int) Math.round(1_000_000_000d / sampleRate)); // sample period in ns
    chunk.putInt(60).putInt(0); // unity note (C3, as the sampler assumes) and pitch fraction
    chunk.putInt(0).putInt(0); // SMPTE format and offset
    chunk.putInt(loops.size()).putInt(0); // number of loops, sampler data
    for (int i = 0; i < loops.size(); i++)
    {
      Loop loop = loops.get(i);
      chunk.putInt(i); // cue point id
      chunk.putInt(loop.alternating() ? 1 : 0); // type: 0 forward, 1 alternating, 2 backward
      chunk.putInt(loop.start());
      chunk.putInt(loop.end() - 1);
      chunk.putInt(0).putInt(0); // fraction, play count (0 = forever)
    }
    return chunk.array();
  }

  /**
   * @return the loops in the file's smpl chunk, empty for a sample without a loop (one shot), or null if the file has
   *         no smpl chunk or isn't a wav file. Backward loops are returned as forward loops.
   */
  public static List<Loop> readLoops(File file) throws IOException
  {
    try (RandomAccessFile in = new RandomAccessFile(file, "r"))
    {
      byte[] riff = new byte[12];
      if (in.length() < riff.length)
      {
        return null;
      }
      in.readFully(riff);
      if (!"RIFF".equals(ascii(riff, 0)) || !"WAVE".equals(ascii(riff, 8)))
      {
        return null;
      }
      long pos = riff.length;
      byte[] chunkHeader = new byte[8];
      while (pos + chunkHeader.length <= in.length())
      {
        in.seek(pos);
        in.readFully(chunkHeader);
        long size = ByteBuffer.wrap(chunkHeader).order(ByteOrder.LITTLE_ENDIAN).getInt(4) & 0xFFFFFFFFL;
        if ("smpl".equals(ascii(chunkHeader, 0)))
        {
          if (size < SMPL_HEADER_SIZE || pos + chunkHeader.length + size > in.length())
          {
            return null;
          }
          byte[] body = new byte[(int) size];
          in.readFully(body);
          return parseSmpl(body);
        }
        pos += chunkHeader.length + size + (size & 1);
      }
      return null;
    }
  }

  static List<Loop> parseSmpl(byte[] body)
  {
    ByteBuffer chunk = ByteBuffer.wrap(body).order(ByteOrder.LITTLE_ENDIAN);
    int numLoops = Math.min(chunk.getInt(28), (body.length - SMPL_HEADER_SIZE) / SMPL_LOOP_SIZE);
    List<Loop> loops = new ArrayList<>();
    for (int i = 0; i < numLoops; i++)
    {
      int offset = SMPL_HEADER_SIZE + i * SMPL_LOOP_SIZE;
      int type = chunk.getInt(offset + 4);
      int start = chunk.getInt(offset + 8);
      int end = chunk.getInt(offset + 12) + 1;
      if (start >= 0 && end > start)
      {
        loops.add(new Loop(start, end, type == 1));
      }
    }
    return loops;
  }

  private static String ascii(byte[] bytes, int offset)
  {
    return new String(bytes, offset, 4, StandardCharsets.US_ASCII);
  }
}
