package axs7sds.dsp;

/**
 * Band limited resampling with a Kaiser windowed sinc kernel. When downsampling, the kernel is widened so it also
 * low-pass filters below the new Nyquist frequency, so frequencies that don't fit the new rate don't alias.
 */
public class Resampler
{
  /** half width of the kernel, in zero crossings */
  private static final int    ZERO_CROSSINGS = 48;
  /** Kaiser window beta, about 86 dB stop band attenuation, more than 12 bits need */
  private static final double BETA           = 8.6;
  /** cutoff as a fraction of the lower Nyquist frequency, leaving room for the transition band */
  private static final double CUTOFF         = 0.9;
  /** kernel table resolution, values per zero crossing */
  private static final int    RESOLUTION     = 512;

  public static double[] resample(double[] in, double fromRate, double toRate)
  {
    double ratio = toRate / fromRate; // output samples per input sample
    double scale = Math.min(1., ratio) * CUTOFF; // cutoff relative to the input Nyquist frequency
    double halfWidth = ZERO_CROSSINGS / scale; // in input samples
    double[] kernel = kernel();
    double[] out = new double[(int) Math.floor(in.length * ratio)];
    for (int j = 0; j < out.length; j++)
    {
      double t = j / ratio; // position in the input
      int first = Math.max(0, (int) Math.ceil(t - halfWidth));
      int last = Math.min(in.length - 1, (int) Math.floor(t + halfWidth));
      double sum = 0;
      for (int k = first; k <= last; k++)
      {
        // linear interpolation in the kernel table, indexed by distance in zero crossings
        double pos = Math.abs(t - k) * scale * RESOLUTION;
        int index = (int) pos;
        if (index < kernel.length - 1)
        {
          double frac = pos - index;
          sum += in[k] * (kernel[index] * (1 - frac) + kernel[index + 1] * frac);
        }
      }
      out[j] = sum * scale;
    }
    return out;
  }

  /**
   * Windowed sinc from 0 to ZERO_CROSSINGS zero crossings
   */
  private static double[] kernel()
  {
    int n = ZERO_CROSSINGS * RESOLUTION + 1;
    double[] kernel = new double[n + 1]; // extra zero so interpolation at the edge is safe
    double i0Beta = besselI0(BETA);
    for (int i = 0; i < n; i++)
    {
      double x = (double) i / RESOLUTION;
      double r = x / ZERO_CROSSINGS;
      double sinc = x == 0 ? 1. : Math.sin(Math.PI * x) / (Math.PI * x);
      kernel[i] = sinc * besselI0(BETA * Math.sqrt(Math.max(0., 1 - r * r))) / i0Beta;
    }
    return kernel;
  }

  /**
   * Modified Bessel function of the first kind, order 0
   */
  private static double besselI0(double x)
  {
    double sum = 1;
    double term = 1;
    for (int k = 1; k < 50 && term > 1e-12 * sum; k++)
    {
      term *= (x / (2 * k)) * (x / (2 * k));
      sum += term;
    }
    return sum;
  }
}
