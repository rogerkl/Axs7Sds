package axs7sds.dsp;

import java.util.Arrays;

import axs7sds.dsp.eduminesjtk.FftComplex;

public class MammothFftLoop
{
  public static int getFftSize(int numSamples, int fadein, int maxSamples)
  {
    if (numSamples + fadein <= maxSamples)
    {
      return FftComplex.nfftSmallLess(numSamples);
    }
    else
    {
      return FftComplex.nfftSmallLess(maxSamples - fadein);
    }
  }

  public static double[] process(double[] data, int n, int fadein, double phaseMult, double binPow)
  {
    double[] fftBuffer = new double[n * 2];
    Arrays.fill(fftBuffer, 0.0f);
    int fi = 0;
    for (int di = 0; di < n; fi += 2, di++)
    {
      fftBuffer[fi] = data[di] * (0.5 * (1. - Math.cos(2. * Math.PI * di / (n /* - 1*/)))); // hann window
    }

    FftComplex fft = new FftComplex(n);

    fft.complexToComplex(1, fftBuffer, fftBuffer);
    //don't want any DC
    fftBuffer[0] = 0;
    fftBuffer[1] = 0;

    double max = 0;

    for (int f = 0; f < fftBuffer.length; f += 2)
    {
      ComplexPolar polar = new ComplexCartesian(fftBuffer[f], fftBuffer[f + 1]).toPolar();
      fftBuffer[f] = polar.r();
      fftBuffer[f + 1] = polar.θ();
      double abs = Math.abs(polar.r());
      if (abs > max)
      {
        max = abs;
      }
    }

    //normalize so that pow works as expected
    if (max > 0)
    {
      for (int f = 0; f < fftBuffer.length; f += 2)
      {
        fftBuffer[f] = fftBuffer[f] / max;
      }
    }

    //process
    for (int f = 0; f < fftBuffer.length; f += 2)
    {
      fftBuffer[f] = Math.pow(fftBuffer[f], binPow);
      fftBuffer[f + 1] = fftBuffer[f + 1] * phaseMult;
    }

    for (int f = 0; f < fftBuffer.length; f += 2)
    {
      ComplexCartesian polar = new ComplexPolar(fftBuffer[f], fftBuffer[f + 1]).toCartesian();
      fftBuffer[f] = polar.real();
      fftBuffer[f + 1] = polar.imag();
    }

    fft.complexToComplex(-1, fftBuffer, fftBuffer);

    max = 0;
    for (int f = 0; f < fftBuffer.length; f += 2)
    {
      double abs = Math.abs(fftBuffer[f]);
      if (abs > max)
      {
        max = abs;
      }
    }
    if (max > 0)
    {
      for (int f = 0; f < fftBuffer.length; f += 2)
      {
        fftBuffer[f] = fftBuffer[f] / max;
      }
    }

    double[] outData = new double[n + fadein];

    fi = 0;
    for (int di = 0; di < n; fi += 2, di++)
    {
      outData[di + fadein] = fftBuffer[fi];
    }

    int start = n;
    for (int i = 0; i < fadein; i++)
    {
      outData[i] = outData[start + i] * (((double) i) / ((double) fadein));
    }

    return outData;
  }

  record ComplexCartesian(double real, double imag)
  {
    ComplexPolar toPolar()
    {
      return new ComplexPolar(Math.sqrt(real * real + imag * imag), Math.atan2(imag, real));
    }
  }

  record ComplexPolar(double r, double θ)
  {
    ComplexCartesian toCartesian()
    {
      return new ComplexCartesian(r * Math.cos(θ), r * Math.sin(θ));
    }
  }
}
