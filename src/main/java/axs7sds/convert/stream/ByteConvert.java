package axs7sds.convert.stream;

public interface ByteConvert
{
  /*
   * Convert bytearray with interleaved samples to double arrays for each channel.
   * out must have the same number of channels as the bytearray.
   */
  public void fromBytes(byte[] in, double[][] out, int frames);

  /*
   * Convert double arrays for each channel to bytearray with interleaved samples
   * in must have the same number of channels as the bytearray.
   */
  public void toBytes(double[][] in, byte[] out, int frames);

  /*
   * Convert bytearray with interleaved samples to double arrays for one channel.
   *
   */
  public void fromBytes(byte[] in, double[] out, int frames, int numchannels, int channel);

  /*
   * Convert double array for one channel to bytearray with interleaved samples
   */
  public void toBytes(double[] in, byte[] out, int frames, int numchannels, int channel);
}
