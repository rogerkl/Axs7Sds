package axs7sds.convert.stream;

import javax.sound.sampled.AudioFormat;

import axs7sds.convert.UnsupportedFormatException;

public class ByteConvertFactory
{
  public static ByteConvert createConverter(final AudioFormat format) throws UnsupportedFormatException
  {
    if (format.getEncoding() == AudioFormat.Encoding.PCM_SIGNED && format.getSampleSizeInBits() >= 8
        && format.getSampleSizeInBits() <= 32)
    {
      return new PCMSIGNED(format.getSampleSizeInBits() / 8, format.isBigEndian());
    }
    if (format.getEncoding() == AudioFormat.Encoding.PCM_FLOAT
        && (format.getSampleSizeInBits() == 32 || format.getSampleSizeInBits() == 64))
    {
      return new IEEEFLOAT(format.getSampleSizeInBits() / 8, format.isBigEndian());
    }
    if (format.getEncoding() == AudioFormat.Encoding.PCM_SIGNED && format.getSampleSizeInBits() == 16
        && !format.isBigEndian())
    {
      return new LE16Bit();
    }
    throw new UnsupportedFormatException("Format not supported");
  }
}