package axs7sds.convert.stream;

public final class LE16Bit extends AbstractByteConverter implements ByteConvert
{
  @Override
  public void fromBytes(final byte[] in, final double[] out, final int frames, final int numchannels,
      final int channel)
  {
    int offset = 0;
    for (int i = 0; i < frames; i++)
    {
      offset = i * numchannels * 2 + channel * 2;
      out[i] = (in[offset + 1] << 8 | in[offset] & 0xFF) / 32768.;
    }
  }

  @Override
  public void toBytes(final double[] in, final byte[] out, final int frames, final int numchannels,
      final int channel)
  {
    int offset = 0;
    for (int i = 0; i < frames; i++)
    {
      offset = i * numchannels * 2 + channel * 2;
      out[offset] = (byte) ((int) (in[i] * 32768.) & 0xFF);
      out[offset + 1] = (byte) ((int) (in[i] * 32768.) >> 8 & 0xFF);
    }
  }
}
