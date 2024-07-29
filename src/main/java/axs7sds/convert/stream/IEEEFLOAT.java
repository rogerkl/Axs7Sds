package axs7sds.convert.stream;

public final class IEEEFLOAT extends AbstractByteConverter implements ByteConvert
{
  int     bytes;
  boolean bigEndian;

  public IEEEFLOAT(final int bytes, final boolean bigEndian)
  {
    this.bytes = bytes;
    this.bigEndian = bigEndian;
  }

  @Override
  public void fromBytes(final byte[] in, final double[] out, final int frames, final int numchannels,
      final int channel)
  {
    int offset = 0;
    if (bytes == 4)
    {
      for (int i = 0; i < frames; i++)
      {
        offset = i * numchannels * bytes + channel * bytes;
        out[i] = getFloat(in, offset);
      }
    }
    else if (bytes == 8)
    {
      for (int i = 0; i < frames; i++)
      {
        offset = i * numchannels * bytes + channel * bytes;
        out[i] = getDouble(in, offset);
      }
    }
  }

  @Override
  public void toBytes(final double[] in, final byte[] out, final int frames, final int numchannels,
      final int channel)
  {
    int offset = 0;
    if (bytes == 4)
    {
      for (int i = 0; i < frames; i++)
      {
        offset = i * numchannels * bytes + channel * bytes;
        fromFloat(in[i], out, offset);
      }
    }
    else if (bytes == 8)
    {
      for (int i = 0; i < frames; i++)
      {
        offset = i * numchannels * bytes + channel * bytes;
        fromDouble(in[i], out, offset);
      }
    }
  }

  private double getFloat(final byte[] b, final int offset)
  {
    double sample = 0;
    int ret = 0;
    int length = bytes;
    for (int i = 0; i < bytes; i++, length--)
    {
      ret |= (b[offset + i] & 0xFF) << (bigEndian ? length : i + 1) * 8 - 8;
    }
    sample = Float.intBitsToFloat(ret);
    return sample;
  }

  private double getDouble(final byte[] b, final int offset)
  {
    double sample = 0;
    long ret = 0;
    int length = bytes;
    for (int i = 0; i < bytes; i++, length--)
    {
      ret |= (b[offset + i] & 0xFF) << (bigEndian ? length : i + 1) * 8 - 8;
    }
    sample = Double.longBitsToDouble(ret);
    return sample;
  }

  private final void fromFloat(final double value, final byte[] b, final int offset)
  {
    int ival = -1;
    ival = Float.floatToIntBits((float) value);
    if (bigEndian)
    {
      b[offset + 0] = (byte) ((ival & 0xff000000) >> 8 * 3);
      b[offset + 1] = (byte) ((ival & 0x00ff0000) >> 8 * 2);
      b[offset + 2] = (byte) ((ival & 0x0000ff00) >> 8);
      b[offset + 3] = (byte) (ival & 0x000000ff);
    }
    else
    {
      b[offset + 0] = (byte) (ival & 0x000000ff);
      b[offset + 1] = (byte) ((ival & 0x0000ff00) >> 8);
      b[offset + 2] = (byte) ((ival & 0x00ff0000) >> 8 * 2);
      b[offset + 3] = (byte) ((ival & 0xff000000) >> 8 * 3);
    }
  }

  private final void fromDouble(final double value, final byte[] b, final int offset)
  {
    long ival = -1;
    ival = Double.doubleToLongBits(value);
    if (bigEndian)
    {
      b[offset + 0] = (byte) ((ival & 0xff000000) >> 8 * 3);
      b[offset + 1] = (byte) ((ival & 0x00ff0000) >> 8 * 2);
      b[offset + 2] = (byte) ((ival & 0x0000ff00) >> 8);
      b[offset + 3] = (byte) (ival & 0x000000ff);
    }
    else
    {
      b[offset + 0] = (byte) (ival & 0x000000ff);
      b[offset + 1] = (byte) ((ival & 0x0000ff00) >> 8);
      b[offset + 2] = (byte) ((ival & 0x00ff0000) >> 8 * 2);
      b[offset + 3] = (byte) ((ival & 0xff000000) >> 8 * 3);
    }
  }
}