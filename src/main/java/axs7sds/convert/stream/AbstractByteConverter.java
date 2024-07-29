package axs7sds.convert.stream;

public abstract sealed class AbstractByteConverter implements ByteConvert permits IEEEFLOAT, LE16Bit, PCMSIGNED
{
  @Override
  public void fromBytes(final byte[] in, final double[][] out, final int frames)
  {
    for (int c = 0; c < out.length; c++)
    {
      fromBytes(in, out[c], frames, out.length, c);
    }
  }

  @Override
  public void toBytes(final double[][] in, final byte[] out, final int frames)
  {
    for (int c = 0; c < in.length; c++)
    {
      toBytes(in[c], out, frames, in.length, c);
    }
  }
}
