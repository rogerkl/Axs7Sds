package axs7sds.dsp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ResamplerTest
{
  private static double[] sine(double frequency, double rate, int n)
  {
    double[] data = new double[n];
    for (int i = 0; i < n; i++)
    {
      data[i] = 0.9 * Math.sin(2 * Math.PI * frequency * i / rate);
    }
    return data;
  }

  /**
   * Largest difference from the ideal sine at the new rate, away from the edges where the filter runs out of data
   */
  private static double maxError(double[] resampled, double frequency, double rate)
  {
    double[] ideal = sine(frequency, rate, resampled.length);
    double max = 0;
    for (int i = 500; i < resampled.length - 500; i++)
    {
      max = Math.max(max, Math.abs(resampled[i] - ideal[i]));
    }
    return max;
  }

  @Test
  public void downsamplingKeepsFrequencyAndLevel()
  {
    double[] out = Resampler.resample(sine(1000, 48000, 20000), 48000, 39555.4);
    assertEquals((int) (20000 * 39555.4 / 48000), out.length);
    double error = maxError(out, 1000, 39555.4);
    assertTrue("error " + error, error < 1e-4); // 12 bits is 4.9e-4 per step
  }

  @Test
  public void upsamplingKeepsFrequencyAndLevel()
  {
    double[] out = Resampler.resample(sine(1000, 22050, 20000), 22050, 22199.53);
    double error = maxError(out, 1000, 22199.53);
    assertTrue("error " + error, error < 1e-4);
  }

  @Test
  public void downsamplingRemovesFrequenciesAboveNewNyquist()
  {
    // 21 kHz doesn't fit at 39555.4 Hz (Nyquist 19.8 kHz) and would alias to 18.6 kHz
    double[] out = Resampler.resample(sine(21000, 48000, 20000), 48000, 39555.4);
    double max = 0;
    for (int i = 500; i < out.length - 500; i++)
    {
      max = Math.max(max, Math.abs(out[i]));
    }
    assertTrue("level " + max, max < 1e-3); // more than 60 dB down
  }
}
