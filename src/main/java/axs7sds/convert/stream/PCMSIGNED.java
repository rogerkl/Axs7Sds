package axs7sds.convert.stream;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.imageio.stream.IIOByteBuffer;

public final class PCMSIGNED extends AbstractByteConverter implements ByteConvert
{
  int     bytes;
  boolean bigEndian;

  public PCMSIGNED(final int bytes, final boolean bigEndian)
  {
    this.bytes = bytes;
    this.bigEndian = bigEndian;
  }

  @Override
  public void fromBytes(final byte[] in, final double[] out, final int frames, final int numchannels,
      final int channel)
  {
    int offset = 0;
    for (int i = 0; i < frames; i++)
    {
      offset = i * numchannels * bytes + channel * bytes;
      out[i] = getDouble(in, offset);
    }
  }

  @Override
  public void toBytes(final double[] in, final byte[] out, final int frames, final int numchannels,
      final int channel)
  {
    int offset = 0;
    for (int i = 0; i < frames; i++)
    {
      offset = i * numchannels * bytes + channel * bytes;
      fromDouble(in[i], out, offset);
    }
  }

  private double getDouble(final byte[] b, final int offset)
  {
    double sample = 0;
    int ret = 0;
    int length = bytes;
    for (int i = 0; i < bytes; i++, length--)
    {
      ret |= (b[offset + i] & 0xFF) << (bigEndian ? length : i + 1) * 8 - 8;
    }
    switch (bytes)
    {
      case 1 :
        if (ret > 0x7F)
        {
          ret = ~ret + 1;
          ret &= 0x7F;
          ret = ~ret + 1;
        }
        sample = (double) ret / (double) Byte.MAX_VALUE;
        break;
      case 2 :
        if (ret > 0x7FFF)
        {
          ret = ~ret + 1;
          ret &= 0x7FFF;
          ret = ~ret + 1;
        }
        sample = (double) ret / (double) Short.MAX_VALUE;
        break;
      case 3 :
        if (ret > 0x7FFFFF)
        {
          ret = ~ret + 1;
          ret &= 0x7FFFFF;
          ret = ~ret + 1;
        }
        sample = (double) ret / 8388608f;
        break;
      case 4 :
        sample = (double) ret / (double) Integer.MAX_VALUE;
        break;
    }
    return sample;
  }

  private final void fromDouble(final double value, final byte[] b, final int offset)
  {
    int ival = -1;
    switch (bytes)
    {
      case 1 : // 8 bit
        b[offset + 0] = Float.valueOf((float) (value * Byte.MAX_VALUE)).byteValue();
        break;
      case 2 : // 16 bit
        final short sval = Float.valueOf((float) (value * Short.MAX_VALUE)).shortValue();
        if (bigEndian)
        {
          b[offset + 0] = (byte) ((sval & 0x0000ff00) >> 8);
          b[offset + 1] = (byte) (sval & 0x000000ff);
        }
        else
        {
          b[offset + 0] = (byte) (sval & 0x000000ff);
          b[offset + 1] = (byte) ((sval & 0x0000ff00) >> 8);
        }
        break;
      case 3 : // 24 bit
        ival = Float.valueOf((float) (value * 8388608)).intValue();
        if (bigEndian)
        {
          b[offset + 0] = (byte) ((ival & 0x00ff0000) >> 8 * 2);
          b[offset + 1] = (byte) ((ival & 0x0000ff00) >> 8);
          b[offset + 2] = (byte) (ival & 0x000000ff);
        }
        else
        {
          b[offset + 0] = (byte) (ival & 0x000000ff);
          b[offset + 1] = (byte) ((ival & 0x0000ff00) >> 8);
          b[offset + 2] = (byte) ((ival & 0x00ff0000) >> 8 * 2);
        }
        break;
      case 4 : // 32 bit
        ival = Float.valueOf((float) (value * Integer.MAX_VALUE)).intValue();
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
        break;
    }
  }

  public static void main(String[] args)
  {
    ByteBuffer byteBuffer = ByteBuffer.allocate(2);
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    PCMSIGNED pcmsigned = new PCMSIGNED(2, false);
    byte[] b = new byte[2];
    double[] da = new double[1];
    for (short i = 0; i < Short.MAX_VALUE; i++)
    {
      byteBuffer.putShort(0, i);
      byteBuffer.get(0, b);
      pcmsigned.fromBytes(b, da, 1, 1, 0);
      pcmsigned.toBytes(da, b, 1, 1, 0);
      byteBuffer.put(0, b);
      short s = byteBuffer.getShort(0);
      if (s != i)
        System.out.println(s);
      if (da[0] != ((double) i) / ((double) Short.MAX_VALUE))
        System.out.println(da[0]);

      byteBuffer.putShort(0, (short) -i);
      byteBuffer.get(0, b);
      pcmsigned.fromBytes(b, da, 1, 1, 0);
      pcmsigned.toBytes(da, b, 1, 1, 0);
      byteBuffer.put(0, b);
      s = byteBuffer.getShort(0);
      if (s != -i)
        System.out.println(s + "<>" + (-i));
      if (da[0] != ((double) -i) / ((double) Short.MAX_VALUE))
        System.out.println(da[0]);

    }
  }
}