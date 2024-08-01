package axs7sds.dsp.eduminesjtk;

/**
 * This class is based on Pffac.java from the MinesJTK library https://github.com/MinesJTK/jtk
 *
 * Prime-factor complex-to-complex FFT. The FFT length nfft must be composed
 * of mutually prime factors from the set {2,3,4,5,7,8,9,11,13,16}. This
 * restriction implies that n cannot exceed 720720 = 5*7*9*11*13*16.
 * <p>
 * References:
 * <ul><li>
 * Temperton, C., 1985, Implementation of a self-sorting in-place prime
 * factor fft algorithm:  Journal of Computational Physics, v. 58,
 * p. 283-299.
 * </li><li>
 * Temperton, C., 1988, A new set of minimum-add rotated rotated dft
 * modules: Journal of Computational Physics, v. 75, p. 190-198.
 * </li></ul>
 * @author Dave Hale, Colorado School of Mines
 * @version 2005.03.21
 */
class Pfac
{
  public static final int MAX_N = 5 * 7 * 9 * 11 * 13 * 16;

  /**
   * Determines whether the specified FFT length is valid.
   * @param nfft the FFT length.
   * @return true, if valid; false, otherwise.
   */
  static boolean nfftValid(int nfft)
  {
    return search(_ntable, nfft) >= 0;
  }

  /**
   * Returns an FFT length optimized for memory. The FFT length will be the
   * smallest valid length that is not less than the specified length n.
   * @param n the lower bound on FFT length.
   * @return the FFT length.
   * @exception IllegalArgumentException if the specified length n exceeds
   *  the maximum length supported by this implementation. Currently, the
   *  maximum length is 720,720.
   */
  static int nfftSmall(int n)
  {
    if (!(n <= MAX_N))
    {
      throw new IllegalArgumentException("required condition: " + "n does not exceed " + MAX_N);
    }
    int itable = search(_ntable, n);
    if (itable < 0)
    {
      itable = -itable;
    }
    return _ntable[itable];
  }

  /**
   * Returns an FFT length optimized for memory. The FFT length will be the
   * highest valid length that is less or equal to the specified length n.
   * @param n the upper bound on FFT length.
   * @return the FFT length.
   * @exception IllegalArgumentException if the specified length n exceeds
   *  the maximum length supported by this implementation. Currently, the
   *  maximum length is 720,720.
   */
  static int nfftSmallLess(int n)
  {
    if (!(n <= MAX_N))
    {
      throw new IllegalArgumentException("required condition: " + "n does not exceed " + MAX_N);
    }
    int itable = search(_ntable, n);
    if (itable < 0)
    {
      itable = -itable - 1;
    }
    return _ntable[itable];
  }

  /**
   * Returns an FFT length optimized for speed. The FFT length will be the
   * fastest valid length that is not less than the specified length n.
   * @param n the lower bound on FFT length.
   * @return the FFT length.
   * @exception IllegalArgumentException if the specified length n exceeds
   *  the maximum length supported by this implementation. Currently, the
   *  maximum length is 720,720.
   */
  static int nfftFast(int n)
  {
    if (!(n <= MAX_N))
    {
      throw new IllegalArgumentException("required condition: " + "n does not exceed " + MAX_N);
    }
    int ifast = search(_ntable, n);
    if (ifast < 0)
    {
      ifast = -ifast;
    }
    int nfast = _ntable[ifast];
    int nstop = 2 * nfast;
    double cfast = _ctable[ifast];
    for (int i = ifast + 1; i < NTABLE && _ntable[i] < nstop; ++i)
    {
      if (_ctable[i] < cfast)
      {
        cfast = _ctable[i];
        nfast = _ntable[i];
      }
    }
    return nfast;
  }

  /**
   * Prime-factor complex-to-complex FFT for 1-D arrays.
   * @param sign the sign of the exponent in the Fourier transform.
   * @param nfft the FFT length.
   * @param z array[2*nfft] of nfft packed complex numbers.
   */
  static void transform(int sign, int nfft, double[] z)
  {

    // What is left of n after dividing by factors.
    int nleft = nfft;

    // Loop over all possible factors, from largest to smallest.
    for (int jfac = 0; jfac < NFAC; ++jfac)
    {

      // Skip the current factor, if not a mutually prime factor of n
      int ifac = _kfac[jfac];
      int ndiv = nleft / ifac;
      if (ndiv * ifac != nleft)
      {
        continue;
      }

      // What is left of n (nleft), and n divided by the current factor (m).
      nleft = ndiv;
      int m = nfft / ifac;

      // Rotation factor mu and stride mm.
      int mu = 0;
      int mm = 0;
      for (int kfac = 1; kfac <= ifac && mm % ifac != 1; ++kfac)
      {
        mu = kfac;
        mm = kfac * m;
      }
      if (sign < 0)
      {
        mu = ifac - mu;
      }

      // Array stride, bound, and indices.
      int jinc = 2 * mm;
      int jmax = 2 * nfft;
      int j0 = 0;
      int j1 = j0 + jinc;

      // Factor 2.
      if (ifac == 2)
      {
        pfa2(z, m, j0, j1);
        continue;
      }
      int j2 = (j1 + jinc) % jmax;

      // Factor 3.
      if (ifac == 3)
      {
        pfa3(z, mu, m, j0, j1, j2);
        continue;
      }
      int j3 = (j2 + jinc) % jmax;

      // Factor 4.
      if (ifac == 4)
      {
        pfa4(z, mu, m, j0, j1, j2, j3);
        continue;
      }
      int j4 = (j3 + jinc) % jmax;

      // Factor 5.
      if (ifac == 5)
      {
        pfa5(z, mu, m, j0, j1, j2, j3, j4);
        continue;
      }
      int j5 = (j4 + jinc) % jmax;
      int j6 = (j5 + jinc) % jmax;

      // Factor 7.
      if (ifac == 7)
      {
        pfa7(z, mu, m, j0, j1, j2, j3, j4, j5, j6);
        continue;
      }
      int j7 = (j6 + jinc) % jmax;

      // Factor 8.
      if (ifac == 8)
      {
        pfa8(z, mu, m, j0, j1, j2, j3, j4, j5, j6, j7);
        continue;
      }
      int j8 = (j7 + jinc) % jmax;

      // Factor 9.
      if (ifac == 9)
      {
        pfa9(z, mu, m, j0, j1, j2, j3, j4, j5, j6, j7, j8);
        continue;
      }
      int j9 = (j8 + jinc) % jmax;
      int j10 = (j9 + jinc) % jmax;

      // Factor 11.
      if (ifac == 11)
      {
        pfa11(z, mu, m, j0, j1, j2, j3, j4, j5, j6, j7, j8, j9, j10);
        continue;
      }
      int j11 = (j10 + jinc) % jmax;
      int j12 = (j11 + jinc) % jmax;

      // Factor 13.
      if (ifac == 13)
      {
        pfa13(z, mu, m, j0, j1, j2, j3, j4, j5, j6, j7, j8, j9, j10, j11, j12);
        continue;
      }
      int j13 = (j12 + jinc) % jmax;
      int j14 = (j13 + jinc) % jmax;
      int j15 = (j14 + jinc) % jmax;

      // Factor 16.
      if (ifac == 16)
      {
        pfa16(z, mu, m, j0, j1, j2, j3, j4, j5, j6, j7, j8, j9, j10, j11, j12, j13, j14, j15);
      }
    }
  }

  private static void pfa2(double[] z, int m, int j0, int j1)
  {
    for (int i = 0; i < m; ++i)
    {
      double t1r = z[j0] - z[j1];
      double t1i = z[j0 + 1] - z[j1 + 1];
      z[j0] = z[j0] + z[j1];
      z[j0 + 1] = z[j0 + 1] + z[j1 + 1];
      z[j1] = t1r;
      z[j1 + 1] = t1i;
      int jt = j1 + 2;
      j1 = j0 + 2;
      j0 = jt;
    }
  }

  private static void pfa3(double[] z, int mu, int m,
      int j0, int j1, int j2)
  {
    double c1;
    if (mu == 1)
    {
      c1 = P866;
    }
    else
    {
      c1 = -P866;
    }
    for (int i = 0; i < m; ++i)
    {
      double t1r = z[j1] + z[j2];
      double t1i = z[j1 + 1] + z[j2 + 1];
      double y1r = z[j0] - 0.5f * t1r;
      double y1i = z[j0 + 1] - 0.5f * t1i;
      double y2r = c1 * (z[j1] - z[j2]);
      double y2i = c1 * (z[j1 + 1] - z[j2 + 1]);
      z[j0] = z[j0] + t1r;
      z[j0 + 1] = z[j0 + 1] + t1i;
      z[j1] = y1r - y2i;
      z[j1 + 1] = y1i + y2r;
      z[j2] = y1r + y2i;
      z[j2 + 1] = y1i - y2r;
      int jt = j2 + 2;
      j2 = j1 + 2;
      j1 = j0 + 2;
      j0 = jt;
    }
  }

  private static void pfa4(double[] z, int mu, int m,
      int j0, int j1, int j2, int j3)
  {
    double c1;
    if (mu == 1)
    {
      c1 = PONE;
    }
    else
    {
      c1 = -PONE;
    }
    for (int i = 0; i < m; ++i)
    {
      double t1r = z[j0] + z[j2];
      double t1i = z[j0 + 1] + z[j2 + 1];
      double t2r = z[j1] + z[j3];
      double t2i = z[j1 + 1] + z[j3 + 1];
      double y1r = z[j0] - z[j2];
      double y1i = z[j0 + 1] - z[j2 + 1];
      double y3r = c1 * (z[j1] - z[j3]);
      double y3i = c1 * (z[j1 + 1] - z[j3 + 1]);
      z[j0] = t1r + t2r;
      z[j0 + 1] = t1i + t2i;
      z[j1] = y1r - y3i;
      z[j1 + 1] = y1i + y3r;
      z[j2] = t1r - t2r;
      z[j2 + 1] = t1i - t2i;
      z[j3] = y1r + y3i;
      z[j3 + 1] = y1i - y3r;
      int jt = j3 + 2;
      j3 = j2 + 2;
      j2 = j1 + 2;
      j1 = j0 + 2;
      j0 = jt;
    }
  }

  private static void pfa5(double[] z, int mu, int m,
      int j0, int j1, int j2, int j3, int j4)
  {
    double c1, c2, c3;
    if (mu == 1)
    {
      c1 = P559;
      c2 = P951;
      c3 = P587;
    }
    else if (mu == 2)
    {
      c1 = -P559;
      c2 = P587;
      c3 = -P951;
    }
    else if (mu == 3)
    {
      c1 = -P559;
      c2 = -P587;
      c3 = P951;
    }
    else
    {
      c1 = P559;
      c2 = -P951;
      c3 = -P587;
    }
    for (int i = 0; i < m; ++i)
    {
      double t1r = z[j1] + z[j4];
      double t1i = z[j1 + 1] + z[j4 + 1];
      double t2r = z[j2] + z[j3];
      double t2i = z[j2 + 1] + z[j3 + 1];
      double t3r = z[j1] - z[j4];
      double t3i = z[j1 + 1] - z[j4 + 1];
      double t4r = z[j2] - z[j3];
      double t4i = z[j2 + 1] - z[j3 + 1];
      double t5r = t1r + t2r;
      double t5i = t1i + t2i;
      double t6r = c1 * (t1r - t2r);
      double t6i = c1 * (t1i - t2i);
      double t7r = z[j0] - 0.25f * t5r;
      double t7i = z[j0 + 1] - 0.25f * t5i;
      double y1r = t7r + t6r;
      double y1i = t7i + t6i;
      double y2r = t7r - t6r;
      double y2i = t7i - t6i;
      double y3r = c3 * t3r - c2 * t4r;
      double y3i = c3 * t3i - c2 * t4i;
      double y4r = c2 * t3r + c3 * t4r;
      double y4i = c2 * t3i + c3 * t4i;
      z[j0] = z[j0] + t5r;
      z[j0 + 1] = z[j0 + 1] + t5i;
      z[j1] = y1r - y4i;
      z[j1 + 1] = y1i + y4r;
      z[j2] = y2r - y3i;
      z[j2 + 1] = y2i + y3r;
      z[j3] = y2r + y3i;
      z[j3 + 1] = y2i - y3r;
      z[j4] = y1r + y4i;
      z[j4 + 1] = y1i - y4r;
      int jt = j4 + 2;
      j4 = j3 + 2;
      j3 = j2 + 2;
      j2 = j1 + 2;
      j1 = j0 + 2;
      j0 = jt;
    }
  }

  private static void pfa7(double[] z, int mu, int m,
      int j0, int j1, int j2, int j3, int j4, int j5, int j6)
  {
    double c1, c2, c3, c4, c5, c6;
    if (mu == 1)
    {
      c1 = P623;
      c2 = -P222;
      c3 = -P900;
      c4 = P781;
      c5 = P974;
      c6 = P433;
    }
    else if (mu == 2)
    {
      c1 = -P222;
      c2 = -P900;
      c3 = P623;
      c4 = P974;
      c5 = -P433;
      c6 = -P781;
    }
    else if (mu == 3)
    {
      c1 = -P900;
      c2 = P623;
      c3 = -P222;
      c4 = P433;
      c5 = -P781;
      c6 = P974;
    }
    else if (mu == 4)
    {
      c1 = -P900;
      c2 = P623;
      c3 = -P222;
      c4 = -P433;
      c5 = P781;
      c6 = -P974;
    }
    else if (mu == 5)
    {
      c1 = -P222;
      c2 = -P900;
      c3 = P623;
      c4 = -P974;
      c5 = P433;
      c6 = P781;
    }
    else
    {
      c1 = P623;
      c2 = -P222;
      c3 = -P900;
      c4 = -P781;
      c5 = -P974;
      c6 = -P433;
    }
    for (int i = 0; i < m; ++i)
    {
      double t1r = z[j1] + z[j6];
      double t1i = z[j1 + 1] + z[j6 + 1];
      double t2r = z[j2] + z[j5];
      double t2i = z[j2 + 1] + z[j5 + 1];
      double t3r = z[j3] + z[j4];
      double t3i = z[j3 + 1] + z[j4 + 1];
      double t4r = z[j1] - z[j6];
      double t4i = z[j1 + 1] - z[j6 + 1];
      double t5r = z[j2] - z[j5];
      double t5i = z[j2 + 1] - z[j5 + 1];
      double t6r = z[j3] - z[j4];
      double t6i = z[j3 + 1] - z[j4 + 1];
      double t7r = z[j0] - 0.5f * t3r;
      double t7i = z[j0 + 1] - 0.5f * t3i;
      double t8r = t1r - t3r;
      double t8i = t1i - t3i;
      double t9r = t2r - t3r;
      double t9i = t2i - t3i;
      double y1r = t7r + c1 * t8r + c2 * t9r;
      double y1i = t7i + c1 * t8i + c2 * t9i;
      double y2r = t7r + c2 * t8r + c3 * t9r;
      double y2i = t7i + c2 * t8i + c3 * t9i;
      double y3r = t7r + c3 * t8r + c1 * t9r;
      double y3i = t7i + c3 * t8i + c1 * t9i;
      double y4r = c6 * t4r - c4 * t5r + c5 * t6r;
      double y4i = c6 * t4i - c4 * t5i + c5 * t6i;
      double y5r = c5 * t4r - c6 * t5r - c4 * t6r;
      double y5i = c5 * t4i - c6 * t5i - c4 * t6i;
      double y6r = c4 * t4r + c5 * t5r + c6 * t6r;
      double y6i = c4 * t4i + c5 * t5i + c6 * t6i;
      z[j0] = z[j0] + t1r + t2r + t3r;
      z[j0 + 1] = z[j0 + 1] + t1i + t2i + t3i;
      z[j1] = y1r - y6i;
      z[j1 + 1] = y1i + y6r;
      z[j2] = y2r - y5i;
      z[j2 + 1] = y2i + y5r;
      z[j3] = y3r - y4i;
      z[j3 + 1] = y3i + y4r;
      z[j4] = y3r + y4i;
      z[j4 + 1] = y3i - y4r;
      z[j5] = y2r + y5i;
      z[j5 + 1] = y2i - y5r;
      z[j6] = y1r + y6i;
      z[j6 + 1] = y1i - y6r;
      int jt = j6 + 2;
      j6 = j5 + 2;
      j5 = j4 + 2;
      j4 = j3 + 2;
      j3 = j2 + 2;
      j2 = j1 + 2;
      j1 = j0 + 2;
      j0 = jt;
    }
  }

  private static void pfa8(double[] z, int mu, int m,
      int j0, int j1, int j2, int j3, int j4, int j5, int j6, int j7)
  {
    double c1, c2, c3;
    if (mu == 1)
    {
      c1 = PONE;
      c2 = P707;
    }
    else if (mu == 3)
    {
      c1 = -PONE;
      c2 = -P707;
    }
    else if (mu == 5)
    {
      c1 = PONE;
      c2 = -P707;
    }
    else
    {
      c1 = -PONE;
      c2 = P707;
    }
    c3 = c1 * c2;
    for (int i = 0; i < m; ++i)
    {
      double t1r = z[j0] + z[j4];
      double t1i = z[j0 + 1] + z[j4 + 1];
      double t2r = z[j0] - z[j4];
      double t2i = z[j0 + 1] - z[j4 + 1];
      double t3r = z[j1] + z[j5];
      double t3i = z[j1 + 1] + z[j5 + 1];
      double t4r = z[j1] - z[j5];
      double t4i = z[j1 + 1] - z[j5 + 1];
      double t5r = z[j2] + z[j6];
      double t5i = z[j2 + 1] + z[j6 + 1];
      double t6r = c1 * (z[j2] - z[j6]);
      double t6i = c1 * (z[j2 + 1] - z[j6 + 1]);
      double t7r = z[j3] + z[j7];
      double t7i = z[j3 + 1] + z[j7 + 1];
      double t8r = z[j3] - z[j7];
      double t8i = z[j3 + 1] - z[j7 + 1];
      double t9r = t1r + t5r;
      double t9i = t1i + t5i;
      double t10r = t3r + t7r;
      double t10i = t3i + t7i;
      double t11r = c2 * (t4r - t8r);
      double t11i = c2 * (t4i - t8i);
      double t12r = c3 * (t4r + t8r);
      double t12i = c3 * (t4i + t8i);
      double y1r = t2r + t11r;
      double y1i = t2i + t11i;
      double y2r = t1r - t5r;
      double y2i = t1i - t5i;
      double y3r = t2r - t11r;
      double y3i = t2i - t11i;
      double y5r = t12r - t6r;
      double y5i = t12i - t6i;
      double y6r = c1 * (t3r - t7r);
      double y6i = c1 * (t3i - t7i);
      double y7r = t12r + t6r;
      double y7i = t12i + t6i;
      z[j0] = t9r + t10r;
      z[j0 + 1] = t9i + t10i;
      z[j1] = y1r - y7i;
      z[j1 + 1] = y1i + y7r;
      z[j2] = y2r - y6i;
      z[j2 + 1] = y2i + y6r;
      z[j3] = y3r - y5i;
      z[j3 + 1] = y3i + y5r;
      z[j4] = t9r - t10r;
      z[j4 + 1] = t9i - t10i;
      z[j5] = y3r + y5i;
      z[j5 + 1] = y3i - y5r;
      z[j6] = y2r + y6i;
      z[j6 + 1] = y2i - y6r;
      z[j7] = y1r + y7i;
      z[j7 + 1] = y1i - y7r;
      int jt = j7 + 2;
      j7 = j6 + 2;
      j6 = j5 + 2;
      j5 = j4 + 2;
      j4 = j3 + 2;
      j3 = j2 + 2;
      j2 = j1 + 2;
      j1 = j0 + 2;
      j0 = jt;
    }
  }

  private static void pfa9(double[] z, int mu, int m,
      int j0, int j1, int j2, int j3, int j4, int j5, int j6, int j7, int j8)
  {
    double c1, c2, c3, c4, c5, c6, c7, c8, c9;
    if (mu == 1)
    {
      c1 = P866;
      c2 = P766;
      c3 = P642;
      c4 = P173;
      c5 = P984;
    }
    else if (mu == 2)
    {
      c1 = -P866;
      c2 = P173;
      c3 = P984;
      c4 = -P939;
      c5 = P342;
    }
    else if (mu == 4)
    {
      c1 = P866;
      c2 = -P939;
      c3 = P342;
      c4 = P766;
      c5 = -P642;
    }
    else if (mu == 5)
    {
      c1 = -P866;
      c2 = -P939;
      c3 = -P342;
      c4 = P766;
      c5 = P642;
    }
    else if (mu == 7)
    {
      c1 = P866;
      c2 = P173;
      c3 = -P984;
      c4 = -P939;
      c5 = -P342;
    }
    else
    {
      c1 = -P866;
      c2 = P766;
      c3 = -P642;
      c4 = P173;
      c5 = -P984;
    }
    c6 = c1 * c2;
    c7 = c1 * c3;
    c8 = c1 * c4;
    c9 = c1 * c5;
    for (int i = 0; i < m; ++i)
    {
      double t1r = z[j3] + z[j6];
      double t1i = z[j3 + 1] + z[j6 + 1];
      double t2r = z[j0] - 0.5f * t1r;
      double t2i = z[j0 + 1] - 0.5f * t1i;
      double t3r = c1 * (z[j3] - z[j6]);
      double t3i = c1 * (z[j3 + 1] - z[j6 + 1]);
      double t4r = z[j0] + t1r;
      double t4i = z[j0 + 1] + t1i;
      double t5r = z[j4] + z[j7];
      double t5i = z[j4 + 1] + z[j7 + 1];
      double t6r = z[j1] - 0.5f * t5r;
      double t6i = z[j1 + 1] - 0.5f * t5i;
      double t7r = z[j4] - z[j7];
      double t7i = z[j4 + 1] - z[j7 + 1];
      double t8r = z[j1] + t5r;
      double t8i = z[j1 + 1] + t5i;
      double t9r = z[j2] + z[j5];
      double t9i = z[j2 + 1] + z[j5 + 1];
      double t10r = z[j8] - 0.5f * t9r;
      double t10i = z[j8 + 1] - 0.5f * t9i;
      double t11r = z[j2] - z[j5];
      double t11i = z[j2 + 1] - z[j5 + 1];
      double t12r = z[j8] + t9r;
      double t12i = z[j8 + 1] + t9i;
      double t13r = t8r + t12r;
      double t13i = t8i + t12i;
      double t14r = t6r + t10r;
      double t14i = t6i + t10i;
      double t15r = t6r - t10r;
      double t15i = t6i - t10i;
      double t16r = t7r + t11r;
      double t16i = t7i + t11i;
      double t17r = t7r - t11r;
      double t17i = t7i - t11i;
      double t18r = c2 * t14r - c7 * t17r;
      double t18i = c2 * t14i - c7 * t17i;
      double t19r = c4 * t14r + c9 * t17r;
      double t19i = c4 * t14i + c9 * t17i;
      double t20r = c3 * t15r + c6 * t16r;
      double t20i = c3 * t15i + c6 * t16i;
      double t21r = c5 * t15r - c8 * t16r;
      double t21i = c5 * t15i - c8 * t16i;
      double t22r = t18r + t19r;
      double t22i = t18i + t19i;
      double t23r = t20r - t21r;
      double t23i = t20i - t21i;
      double y1r = t2r + t18r;
      double y1i = t2i + t18i;
      double y2r = t2r + t19r;
      double y2i = t2i + t19i;
      double y3r = t4r - 0.5f * t13r;
      double y3i = t4i - 0.5f * t13i;
      double y4r = t2r - t22r;
      double y4i = t2i - t22i;
      double y5r = t3r - t23r;
      double y5i = t3i - t23i;
      double y6r = c1 * (t8r - t12r);
      double y6i = c1 * (t8i - t12i);
      double y7r = t21r - t3r;
      double y7i = t21i - t3i;
      double y8r = t3r + t20r;
      double y8i = t3i + t20i;
      z[j0] = t4r + t13r;
      z[j0 + 1] = t4i + t13i;
      z[j1] = y1r - y8i;
      z[j1 + 1] = y1i + y8r;
      z[j2] = y2r - y7i;
      z[j2 + 1] = y2i + y7r;
      z[j3] = y3r - y6i;
      z[j3 + 1] = y3i + y6r;
      z[j4] = y4r - y5i;
      z[j4 + 1] = y4i + y5r;
      z[j5] = y4r + y5i;
      z[j5 + 1] = y4i - y5r;
      z[j6] = y3r + y6i;
      z[j6 + 1] = y3i - y6r;
      z[j7] = y2r + y7i;
      z[j7 + 1] = y2i - y7r;
      z[j8] = y1r + y8i;
      z[j8 + 1] = y1i - y8r;
      int jt = j8 + 2;
      j8 = j7 + 2;
      j7 = j6 + 2;
      j6 = j5 + 2;
      j5 = j4 + 2;
      j4 = j3 + 2;
      j3 = j2 + 2;
      j2 = j1 + 2;
      j1 = j0 + 2;
      j0 = jt;
    }
  }

  private static void pfa11(double[] z, int mu, int m,
      int j0, int j1, int j2, int j3, int j4, int j5,
      int j6, int j7, int j8, int j9, int j10)
  {
    double c1, c2, c3, c4, c5, c6, c7, c8, c9, c10;
    if (mu == 1)
    {
      c1 = P841;
      c2 = P415;
      c3 = -P142;
      c4 = -P654;
      c5 = -P959;
      c6 = P540;
      c7 = P909;
      c8 = P989;
      c9 = P755;
      c10 = P281;
    }
    else if (mu == 2)
    {
      c1 = P415;
      c2 = -P654;
      c3 = -P959;
      c4 = -P142;
      c5 = P841;
      c6 = P909;
      c7 = P755;
      c8 = -P281;
      c9 = -P989;
      c10 = -P540;
    }
    else if (mu == 3)
    {
      c1 = -P142;
      c2 = -P959;
      c3 = P415;
      c4 = P841;
      c5 = -P654;
      c6 = P989;
      c7 = -P281;
      c8 = -P909;
      c9 = P540;
      c10 = P755;
    }
    else if (mu == 4)
    {
      c1 = -P654;
      c2 = -P142;
      c3 = P841;
      c4 = -P959;
      c5 = P415;
      c6 = P755;
      c7 = -P989;
      c8 = P540;
      c9 = P281;
      c10 = -P909;
    }
    else if (mu == 5)
    {
      c1 = -P959;
      c2 = P841;
      c3 = -P654;
      c4 = P415;
      c5 = -P142;
      c6 = P281;
      c7 = -P540;
      c8 = P755;
      c9 = -P909;
      c10 = P989;
    }
    else if (mu == 6)
    {
      c1 = -P959;
      c2 = P841;
      c3 = -P654;
      c4 = P415;
      c5 = -P142;
      c6 = -P281;
      c7 = P540;
      c8 = -P755;
      c9 = P909;
      c10 = -P989;
    }
    else if (mu == 7)
    {
      c1 = -P654;
      c2 = -P142;
      c3 = P841;
      c4 = -P959;
      c5 = P415;
      c6 = -P755;
      c7 = P989;
      c8 = -P540;
      c9 = -P281;
      c10 = P909;
    }
    else if (mu == 8)
    {
      c1 = -P142;
      c2 = -P959;
      c3 = P415;
      c4 = P841;
      c5 = -P654;
      c6 = -P989;
      c7 = P281;
      c8 = P909;
      c9 = -P540;
      c10 = -P755;
    }
    else if (mu == 9)
    {
      c1 = P415;
      c2 = -P654;
      c3 = -P959;
      c4 = -P142;
      c5 = P841;
      c6 = -P909;
      c7 = -P755;
      c8 = P281;
      c9 = P989;
      c10 = P540;
    }
    else
    {
      c1 = P841;
      c2 = P415;
      c3 = -P142;
      c4 = -P654;
      c5 = -P959;
      c6 = -P540;
      c7 = -P909;
      c8 = -P989;
      c9 = -P755;
      c10 = -P281;
    }
    for (int i = 0; i < m; ++i)
    {
      double t1r = z[j1] + z[j10];
      double t1i = z[j1 + 1] + z[j10 + 1];
      double t2r = z[j2] + z[j9];
      double t2i = z[j2 + 1] + z[j9 + 1];
      double t3r = z[j3] + z[j8];
      double t3i = z[j3 + 1] + z[j8 + 1];
      double t4r = z[j4] + z[j7];
      double t4i = z[j4 + 1] + z[j7 + 1];
      double t5r = z[j5] + z[j6];
      double t5i = z[j5 + 1] + z[j6 + 1];
      double t6r = z[j1] - z[j10];
      double t6i = z[j1 + 1] - z[j10 + 1];
      double t7r = z[j2] - z[j9];
      double t7i = z[j2 + 1] - z[j9 + 1];
      double t8r = z[j3] - z[j8];
      double t8i = z[j3 + 1] - z[j8 + 1];
      double t9r = z[j4] - z[j7];
      double t9i = z[j4 + 1] - z[j7 + 1];
      double t10r = z[j5] - z[j6];
      double t10i = z[j5 + 1] - z[j6 + 1];
      double t11r = z[j0] - 0.5f * t5r;
      double t11i = z[j0 + 1] - 0.5f * t5i;
      double t12r = t1r - t5r;
      double t12i = t1i - t5i;
      double t13r = t2r - t5r;
      double t13i = t2i - t5i;
      double t14r = t3r - t5r;
      double t14i = t3i - t5i;
      double t15r = t4r - t5r;
      double t15i = t4i - t5i;
      double y1r = t11r + c1 * t12r + c2 * t13r + c3 * t14r + c4 * t15r;
      double y1i = t11i + c1 * t12i + c2 * t13i + c3 * t14i + c4 * t15i;
      double y2r = t11r + c2 * t12r + c4 * t13r + c5 * t14r + c3 * t15r;
      double y2i = t11i + c2 * t12i + c4 * t13i + c5 * t14i + c3 * t15i;
      double y3r = t11r + c3 * t12r + c5 * t13r + c2 * t14r + c1 * t15r;
      double y3i = t11i + c3 * t12i + c5 * t13i + c2 * t14i + c1 * t15i;
      double y4r = t11r + c4 * t12r + c3 * t13r + c1 * t14r + c5 * t15r;
      double y4i = t11i + c4 * t12i + c3 * t13i + c1 * t14i + c5 * t15i;
      double y5r = t11r + c5 * t12r + c1 * t13r + c4 * t14r + c2 * t15r;
      double y5i = t11i + c5 * t12i + c1 * t13i + c4 * t14i + c2 * t15i;
      double y6r = c10 * t6r - c6 * t7r + c9 * t8r - c7 * t9r + c8 * t10r;
      double y6i = c10 * t6i - c6 * t7i + c9 * t8i - c7 * t9i + c8 * t10i;
      double y7r = c9 * t6r - c8 * t7r + c6 * t8r + c10 * t9r - c7 * t10r;
      double y7i = c9 * t6i - c8 * t7i + c6 * t8i + c10 * t9i - c7 * t10i;
      double y8r = c8 * t6r - c10 * t7r - c7 * t8r + c6 * t9r + c9 * t10r;
      double y8i = c8 * t6i - c10 * t7i - c7 * t8i + c6 * t9i + c9 * t10i;
      double y9r = c7 * t6r + c9 * t7r - c10 * t8r - c8 * t9r - c6 * t10r;
      double y9i = c7 * t6i + c9 * t7i - c10 * t8i - c8 * t9i - c6 * t10i;
      double y10r = c6 * t6r + c7 * t7r + c8 * t8r + c9 * t9r + c10 * t10r;
      double y10i = c6 * t6i + c7 * t7i + c8 * t8i + c9 * t9i + c10 * t10i;
      z[j0] = z[j0] + t1r + t2r + t3r + t4r + t5r;
      z[j0 + 1] = z[j0 + 1] + t1i + t2i + t3i + t4i + t5i;
      z[j1] = y1r - y10i;
      z[j1 + 1] = y1i + y10r;
      z[j2] = y2r - y9i;
      z[j2 + 1] = y2i + y9r;
      z[j3] = y3r - y8i;
      z[j3 + 1] = y3i + y8r;
      z[j4] = y4r - y7i;
      z[j4 + 1] = y4i + y7r;
      z[j5] = y5r - y6i;
      z[j5 + 1] = y5i + y6r;
      z[j6] = y5r + y6i;
      z[j6 + 1] = y5i - y6r;
      z[j7] = y4r + y7i;
      z[j7 + 1] = y4i - y7r;
      z[j8] = y3r + y8i;
      z[j8 + 1] = y3i - y8r;
      z[j9] = y2r + y9i;
      z[j9 + 1] = y2i - y9r;
      z[j10] = y1r + y10i;
      z[j10 + 1] = y1i - y10r;
      int jt = j10 + 2;
      j10 = j9 + 2;
      j9 = j8 + 2;
      j8 = j7 + 2;
      j7 = j6 + 2;
      j6 = j5 + 2;
      j5 = j4 + 2;
      j4 = j3 + 2;
      j3 = j2 + 2;
      j2 = j1 + 2;
      j1 = j0 + 2;
      j0 = jt;
    }
  }

  private static void pfa13(double[] z, int mu, int m,
      int j0, int j1, int j2, int j3, int j4, int j5, int j6,
      int j7, int j8, int j9, int j10, int j11, int j12)
  {
    double c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12;
    if (mu == 1)
    {
      c1 = P885;
      c2 = P568;
      c3 = P120;
      c4 = -P354;
      c5 = -P748;
      c6 = -P970;
      c7 = P464;
      c8 = P822;
      c9 = P992;
      c10 = P935;
      c11 = P663;
      c12 = P239;
    }
    else if (mu == 2)
    {
      c1 = P568;
      c2 = -P354;
      c3 = -P970;
      c4 = -P748;
      c5 = P120;
      c6 = P885;
      c7 = P822;
      c8 = P935;
      c9 = P239;
      c10 = -P663;
      c11 = -P992;
      c12 = -P464;
    }
    else if (mu == 3)
    {
      c1 = P120;
      c2 = -P970;
      c3 = -P354;
      c4 = P885;
      c5 = P568;
      c6 = -P748;
      c7 = P992;
      c8 = P239;
      c9 = -P935;
      c10 = -P464;
      c11 = P822;
      c12 = P663;
    }
    else if (mu == 4)
    {
      c1 = -P354;
      c2 = -P748;
      c3 = P885;
      c4 = P120;
      c5 = -P970;
      c6 = P568;
      c7 = P935;
      c8 = -P663;
      c9 = -P464;
      c10 = P992;
      c11 = -P239;
      c12 = -P822;
    }
    else if (mu == 5)
    {
      c1 = -P748;
      c2 = P120;
      c3 = P568;
      c4 = -P970;
      c5 = P885;
      c6 = -P354;
      c7 = P663;
      c8 = -P992;
      c9 = P822;
      c10 = -P239;
      c11 = -P464;
      c12 = P935;
    }
    else if (mu == 6)
    {
      c1 = -P970;
      c2 = P885;
      c3 = -P748;
      c4 = P568;
      c5 = -P354;
      c6 = P120;
      c7 = P239;
      c8 = -P464;
      c9 = P663;
      c10 = -P822;
      c11 = P935;
      c12 = -P992;
    }
    else if (mu == 7)
    {
      c1 = -P970;
      c2 = P885;
      c3 = -P748;
      c4 = P568;
      c5 = -P354;
      c6 = P120;
      c7 = -P239;
      c8 = P464;
      c9 = -P663;
      c10 = P822;
      c11 = -P935;
      c12 = P992;
    }
    else if (mu == 8)
    {
      c1 = -P748;
      c2 = P120;
      c3 = P568;
      c4 = -P970;
      c5 = P885;
      c6 = -P354;
      c7 = -P663;
      c8 = P992;
      c9 = -P822;
      c10 = P239;
      c11 = P464;
      c12 = -P935;
    }
    else if (mu == 9)
    {
      c1 = -P354;
      c2 = -P748;
      c3 = P885;
      c4 = P120;
      c5 = -P970;
      c6 = P568;
      c7 = -P935;
      c8 = P663;
      c9 = P464;
      c10 = -P992;
      c11 = P239;
      c12 = P822;
    }
    else if (mu == 10)
    {
      c1 = P120;
      c2 = -P970;
      c3 = -P354;
      c4 = P885;
      c5 = P568;
      c6 = -P748;
      c7 = -P992;
      c8 = -P239;
      c9 = P935;
      c10 = P464;
      c11 = -P822;
      c12 = -P663;
    }
    else if (mu == 11)
    {
      c1 = P568;
      c2 = -P354;
      c3 = -P970;
      c4 = -P748;
      c5 = P120;
      c6 = P885;
      c7 = -P822;
      c8 = -P935;
      c9 = -P239;
      c10 = P663;
      c11 = P992;
      c12 = P464;
    }
    else
    {
      c1 = P885;
      c2 = P568;
      c3 = P120;
      c4 = -P354;
      c5 = -P748;
      c6 = -P970;
      c7 = -P464;
      c8 = -P822;
      c9 = -P992;
      c10 = -P935;
      c11 = -P663;
      c12 = -P239;
    }
    for (int i = 0; i < m; ++i)
    {
      double t1r = z[j1] + z[j12];
      double t1i = z[j1 + 1] + z[j12 + 1];
      double t2r = z[j2] + z[j11];
      double t2i = z[j2 + 1] + z[j11 + 1];
      double t3r = z[j3] + z[j10];
      double t3i = z[j3 + 1] + z[j10 + 1];
      double t4r = z[j4] + z[j9];
      double t4i = z[j4 + 1] + z[j9 + 1];
      double t5r = z[j5] + z[j8];
      double t5i = z[j5 + 1] + z[j8 + 1];
      double t6r = z[j6] + z[j7];
      double t6i = z[j6 + 1] + z[j7 + 1];
      double t7r = z[j1] - z[j12];
      double t7i = z[j1 + 1] - z[j12 + 1];
      double t8r = z[j2] - z[j11];
      double t8i = z[j2 + 1] - z[j11 + 1];
      double t9r = z[j3] - z[j10];
      double t9i = z[j3 + 1] - z[j10 + 1];
      double t10r = z[j4] - z[j9];
      double t10i = z[j4 + 1] - z[j9 + 1];
      double t11r = z[j5] - z[j8];
      double t11i = z[j5 + 1] - z[j8 + 1];
      double t12r = z[j6] - z[j7];
      double t12i = z[j6 + 1] - z[j7 + 1];
      double t13r = z[j0] - 0.5f * t6r;
      double t13i = z[j0 + 1] - 0.5f * t6i;
      double t14r = t1r - t6r;
      double t14i = t1i - t6i;
      double t15r = t2r - t6r;
      double t15i = t2i - t6i;
      double t16r = t3r - t6r;
      double t16i = t3i - t6i;
      double t17r = t4r - t6r;
      double t17i = t4i - t6i;
      double t18r = t5r - t6r;
      double t18i = t5i - t6i;
      double y1r = t13r + c1 * t14r + c2 * t15r + c3 * t16r + c4 * t17r + c5 * t18r;
      double y1i = t13i + c1 * t14i + c2 * t15i + c3 * t16i + c4 * t17i + c5 * t18i;
      double y2r = t13r + c2 * t14r + c4 * t15r + c6 * t16r + c5 * t17r + c3 * t18r;
      double y2i = t13i + c2 * t14i + c4 * t15i + c6 * t16i + c5 * t17i + c3 * t18i;
      double y3r = t13r + c3 * t14r + c6 * t15r + c4 * t16r + c1 * t17r + c2 * t18r;
      double y3i = t13i + c3 * t14i + c6 * t15i + c4 * t16i + c1 * t17i + c2 * t18i;
      double y4r = t13r + c4 * t14r + c5 * t15r + c1 * t16r + c3 * t17r + c6 * t18r;
      double y4i = t13i + c4 * t14i + c5 * t15i + c1 * t16i + c3 * t17i + c6 * t18i;
      double y5r = t13r + c5 * t14r + c3 * t15r + c2 * t16r + c6 * t17r + c1 * t18r;
      double y5i = t13i + c5 * t14i + c3 * t15i + c2 * t16i + c6 * t17i + c1 * t18i;
      double y6r = t13r + c6 * t14r + c1 * t15r + c5 * t16r + c2 * t17r + c4 * t18r;
      double y6i = t13i + c6 * t14i + c1 * t15i + c5 * t16i + c2 * t17i + c4 * t18i;
      double y7r = c12 * t7r - c7 * t8r + c11 * t9r - c8 * t10r + c10 * t11r - c9 * t12r;
      double y7i = c12 * t7i - c7 * t8i + c11 * t9i - c8 * t10i + c10 * t11i - c9 * t12i;
      double y8r = c11 * t7r - c9 * t8r + c8 * t9r - c12 * t10r - c7 * t11r + c10 * t12r;
      double y8i = c11 * t7i - c9 * t8i + c8 * t9i - c12 * t10i - c7 * t11i + c10 * t12i;
      double y9r = c10 * t7r - c11 * t8r - c7 * t9r + c9 * t10r - c12 * t11r - c8 * t12r;
      double y9i = c10 * t7i - c11 * t8i - c7 * t9i + c9 * t10i - c12 * t11i - c8 * t12i;
      double y10r = c9 * t7r + c12 * t8r - c10 * t9r - c7 * t10r + c8 * t11r + c11 * t12r;
      double y10i = c9 * t7i + c12 * t8i - c10 * t9i - c7 * t10i + c8 * t11i + c11 * t12i;
      double y11r = c8 * t7r + c10 * t8r + c12 * t9r - c11 * t10r - c9 * t11r - c7 * t12r;
      double y11i = c8 * t7i + c10 * t8i + c12 * t9i - c11 * t10i - c9 * t11i - c7 * t12i;
      double y12r = c7 * t7r + c8 * t8r + c9 * t9r + c10 * t10r + c11 * t11r + c12 * t12r;
      double y12i = c7 * t7i + c8 * t8i + c9 * t9i + c10 * t10i + c11 * t11i + c12 * t12i;
      z[j0] = z[j0] + t1r + t2r + t3r + t4r + t5r + t6r;
      z[j0 + 1] = z[j0 + 1] + t1i + t2i + t3i + t4i + t5i + t6i;
      z[j1] = y1r - y12i;
      z[j1 + 1] = y1i + y12r;
      z[j2] = y2r - y11i;
      z[j2 + 1] = y2i + y11r;
      z[j3] = y3r - y10i;
      z[j3 + 1] = y3i + y10r;
      z[j4] = y4r - y9i;
      z[j4 + 1] = y4i + y9r;
      z[j5] = y5r - y8i;
      z[j5 + 1] = y5i + y8r;
      z[j6] = y6r - y7i;
      z[j6 + 1] = y6i + y7r;
      z[j7] = y6r + y7i;
      z[j7 + 1] = y6i - y7r;
      z[j8] = y5r + y8i;
      z[j8 + 1] = y5i - y8r;
      z[j9] = y4r + y9i;
      z[j9 + 1] = y4i - y9r;
      z[j10] = y3r + y10i;
      z[j10 + 1] = y3i - y10r;
      z[j11] = y2r + y11i;
      z[j11 + 1] = y2i - y11r;
      z[j12] = y1r + y12i;
      z[j12 + 1] = y1i - y12r;
      int jt = j12 + 2;
      j12 = j11 + 2;
      j11 = j10 + 2;
      j10 = j9 + 2;
      j9 = j8 + 2;
      j8 = j7 + 2;
      j7 = j6 + 2;
      j6 = j5 + 2;
      j5 = j4 + 2;
      j4 = j3 + 2;
      j3 = j2 + 2;
      j2 = j1 + 2;
      j1 = j0 + 2;
      j0 = jt;
    }
  }

  private static void pfa16(double[] z, int mu, int m,
      int j0, int j1, int j2, int j3, int j4, int j5, int j6, int j7, int j8,
      int j9, int j10, int j11, int j12, int j13, int j14, int j15)
  {
    double c1, c2, c3, c4, c5, c6, c7;
    if (mu == 1)
    {
      c1 = PONE;
      c2 = P923;
      c3 = P382;
      c4 = P707;
    }
    else if (mu == 3)
    {
      c1 = -PONE;
      c2 = P382;
      c3 = P923;
      c4 = -P707;
    }
    else if (mu == 5)
    {
      c1 = PONE;
      c2 = -P382;
      c3 = P923;
      c4 = -P707;
    }
    else if (mu == 7)
    {
      c1 = -PONE;
      c2 = -P923;
      c3 = P382;
      c4 = P707;
    }
    else if (mu == 9)
    {
      c1 = PONE;
      c2 = -P923;
      c3 = -P382;
      c4 = P707;
    }
    else if (mu == 11)
    {
      c1 = -PONE;
      c2 = -P382;
      c3 = -P923;
      c4 = -P707;
    }
    else if (mu == 13)
    {
      c1 = PONE;
      c2 = P382;
      c3 = -P923;
      c4 = -P707;
    }
    else
    {
      c1 = -PONE;
      c2 = P923;
      c3 = -P382;
      c4 = P707;
    }
    c5 = c1 * c4;
    c6 = c1 * c3;
    c7 = c1 * c2;
    for (int i = 0; i < m; ++i)
    {
      double t1r = z[j0] + z[j8];
      double t1i = z[j0 + 1] + z[j8 + 1];
      double t2r = z[j4] + z[j12];
      double t2i = z[j4 + 1] + z[j12 + 1];
      double t3r = z[j0] - z[j8];
      double t3i = z[j0 + 1] - z[j8 + 1];
      double t4r = c1 * (z[j4] - z[j12]);
      double t4i = c1 * (z[j4 + 1] - z[j12 + 1]);
      double t5r = t1r + t2r;
      double t5i = t1i + t2i;
      double t6r = t1r - t2r;
      double t6i = t1i - t2i;
      double t7r = z[j1] + z[j9];
      double t7i = z[j1 + 1] + z[j9 + 1];
      double t8r = z[j5] + z[j13];
      double t8i = z[j5 + 1] + z[j13 + 1];
      double t9r = z[j1] - z[j9];
      double t9i = z[j1 + 1] - z[j9 + 1];
      double t10r = z[j5] - z[j13];
      double t10i = z[j5 + 1] - z[j13 + 1];
      double t11r = t7r + t8r;
      double t11i = t7i + t8i;
      double t12r = t7r - t8r;
      double t12i = t7i - t8i;
      double t13r = z[j2] + z[j10];
      double t13i = z[j2 + 1] + z[j10 + 1];
      double t14r = z[j6] + z[j14];
      double t14i = z[j6 + 1] + z[j14 + 1];
      double t15r = z[j2] - z[j10];
      double t15i = z[j2 + 1] - z[j10 + 1];
      double t16r = z[j6] - z[j14];
      double t16i = z[j6 + 1] - z[j14 + 1];
      double t17r = t13r + t14r;
      double t17i = t13i + t14i;
      double t18r = c4 * (t15r - t16r);
      double t18i = c4 * (t15i - t16i);
      double t19r = c5 * (t15r + t16r);
      double t19i = c5 * (t15i + t16i);
      double t20r = c1 * (t13r - t14r);
      double t20i = c1 * (t13i - t14i);
      double t21r = z[j3] + z[j11];
      double t21i = z[j3 + 1] + z[j11 + 1];
      double t22r = z[j7] + z[j15];
      double t22i = z[j7 + 1] + z[j15 + 1];
      double t23r = z[j3] - z[j11];
      double t23i = z[j3 + 1] - z[j11 + 1];
      double t24r = z[j7] - z[j15];
      double t24i = z[j7 + 1] - z[j15 + 1];
      double t25r = t21r + t22r;
      double t25i = t21i + t22i;
      double t26r = t21r - t22r;
      double t26i = t21i - t22i;
      double t27r = t9r + t24r;
      double t27i = t9i + t24i;
      double t28r = t10r + t23r;
      double t28i = t10i + t23i;
      double t29r = t9r - t24r;
      double t29i = t9i - t24i;
      double t30r = t10r - t23r;
      double t30i = t10i - t23i;
      double t31r = t5r + t17r;
      double t31i = t5i + t17i;
      double t32r = t11r + t25r;
      double t32i = t11i + t25i;
      double t33r = t3r + t18r;
      double t33i = t3i + t18i;
      double t34r = c2 * t29r - c6 * t30r;
      double t34i = c2 * t29i - c6 * t30i;
      double t35r = t3r - t18r;
      double t35i = t3i - t18i;
      double t36r = c7 * t27r - c3 * t28r;
      double t36i = c7 * t27i - c3 * t28i;
      double t37r = t4r + t19r;
      double t37i = t4i + t19i;
      double t38r = c3 * t27r + c7 * t28r;
      double t38i = c3 * t27i + c7 * t28i;
      double t39r = t4r - t19r;
      double t39i = t4i - t19i;
      double t40r = c6 * t29r + c2 * t30r;
      double t40i = c6 * t29i + c2 * t30i;
      double t41r = c4 * (t12r - t26r);
      double t41i = c4 * (t12i - t26i);
      double t42r = c5 * (t12r + t26r);
      double t42i = c5 * (t12i + t26i);
      double y1r = t33r + t34r;
      double y1i = t33i + t34i;
      double y2r = t6r + t41r;
      double y2i = t6i + t41i;
      double y3r = t35r + t40r;
      double y3i = t35i + t40i;
      double y4r = t5r - t17r;
      double y4i = t5i - t17i;
      double y5r = t35r - t40r;
      double y5i = t35i - t40i;
      double y6r = t6r - t41r;
      double y6i = t6i - t41i;
      double y7r = t33r - t34r;
      double y7i = t33i - t34i;
      double y9r = t38r - t37r;
      double y9i = t38i - t37i;
      double y10r = t42r - t20r;
      double y10i = t42i - t20i;
      double y11r = t36r + t39r;
      double y11i = t36i + t39i;
      double y12r = c1 * (t11r - t25r);
      double y12i = c1 * (t11i - t25i);
      double y13r = t36r - t39r;
      double y13i = t36i - t39i;
      double y14r = t42r + t20r;
      double y14i = t42i + t20i;
      double y15r = t38r + t37r;
      double y15i = t38i + t37i;
      z[j0] = t31r + t32r;
      z[j0 + 1] = t31i + t32i;
      z[j1] = y1r - y15i;
      z[j1 + 1] = y1i + y15r;
      z[j2] = y2r - y14i;
      z[j2 + 1] = y2i + y14r;
      z[j3] = y3r - y13i;
      z[j3 + 1] = y3i + y13r;
      z[j4] = y4r - y12i;
      z[j4 + 1] = y4i + y12r;
      z[j5] = y5r - y11i;
      z[j5 + 1] = y5i + y11r;
      z[j6] = y6r - y10i;
      z[j6 + 1] = y6i + y10r;
      z[j7] = y7r - y9i;
      z[j7 + 1] = y7i + y9r;
      z[j8] = t31r - t32r;
      z[j8 + 1] = t31i - t32i;
      z[j9] = y7r + y9i;
      z[j9 + 1] = y7i - y9r;
      z[j10] = y6r + y10i;
      z[j10 + 1] = y6i - y10r;
      z[j11] = y5r + y11i;
      z[j11 + 1] = y5i - y11r;
      z[j12] = y4r + y12i;
      z[j12 + 1] = y4i - y12r;
      z[j13] = y3r + y13i;
      z[j13 + 1] = y3i - y13r;
      z[j14] = y2r + y14i;
      z[j14 + 1] = y2i - y14r;
      z[j15] = y1r + y15i;
      z[j15 + 1] = y1i - y15r;
      int jt = j15 + 2;
      j15 = j14 + 2;
      j14 = j13 + 2;
      j13 = j12 + 2;
      j12 = j11 + 2;
      j11 = j10 + 2;
      j10 = j9 + 2;
      j9 = j8 + 2;
      j8 = j7 + 2;
      j7 = j6 + 2;
      j6 = j5 + 2;
      j5 = j4 + 2;
      j4 = j3 + 2;
      j3 = j2 + 2;
      j2 = j1 + 2;
      j1 = j0 + 2;
      j0 = jt;
    }
  }

  /**
   * Prime-factor complex-to-complex multiple FFT. Performs multiple
   * transforms across the 2nd (slowest) dimension of a 2-D array.
   * In this version, z[0:nfft-1][0,2,4,...] contains the real parts,
   * and z[0:nfft-1][1,3,5,...] contains the imaginary parts.
   * @param sign the sign of the exponent in the Fourier transform.
   * @param n1 the number of transforms (fast dimension).
   * @param nfft the FFT length (slow dimension).
   * @param z array[nfft][2*n1] of n1*nfft packed complex numbers.
   */
  static void transform2a(int sign, int n1, int nfft, double[][] z)
  {

    // What is left of n after dividing by factors.
    int nleft = nfft;

    // Loop over all possible factors, from largest to smallest.
    for (int jfac = 0; jfac < NFAC; ++jfac)
    {

      // Skip the current factor, if not a mutually prime factor of n
      int ifac = _kfac[jfac];
      int ndiv = nleft / ifac;
      if (ndiv * ifac != nleft)
      {
        continue;
      }

      // What is left of n (nleft), and n divided by the current factor (m).
      nleft = ndiv;
      int m = nfft / ifac;

      // Rotation factor mu and stride mm.
      int mu = 0;
      int mm = 0;
      for (int kfac = 1; kfac <= ifac && mm % ifac != 1; ++kfac)
      {
        mu = kfac;
        mm = kfac * m;
      }
      if (sign < 0)
      {
        mu = ifac - mu;
      }

      // Array stride, bound, and indices.
      int jinc = mm;
      int jmax = nfft;
      int j0 = 0;
      int j1 = j0 + jinc;

      // Factor 2.
      if (ifac == 2)
      {
        pfa2a(n1, z, m, j0, j1);
        continue;
      }
      int j2 = (j1 + jinc) % jmax;

      // Factor 3.
      if (ifac == 3)
      {
        pfa3a(n1, z, mu, m, j0, j1, j2);
        continue;
      }
      int j3 = (j2 + jinc) % jmax;

      // Factor 4.
      if (ifac == 4)
      {
        pfa4a(n1, z, mu, m, j0, j1, j2, j3);
        continue;
      }
      int j4 = (j3 + jinc) % jmax;

      // Factor 5.
      if (ifac == 5)
      {
        pfa5a(n1, z, mu, m, j0, j1, j2, j3, j4);
        continue;
      }
      int j5 = (j4 + jinc) % jmax;
      int j6 = (j5 + jinc) % jmax;

      // Factor 7.
      if (ifac == 7)
      {
        pfa7a(n1, z, mu, m, j0, j1, j2, j3, j4, j5, j6);
        continue;
      }
      int j7 = (j6 + jinc) % jmax;

      // Factor 8.
      if (ifac == 8)
      {
        pfa8a(n1, z, mu, m, j0, j1, j2, j3, j4, j5, j6, j7);
        continue;
      }
      int j8 = (j7 + jinc) % jmax;

      // Factor 9.
      if (ifac == 9)
      {
        pfa9a(n1, z, mu, m, j0, j1, j2, j3, j4, j5, j6, j7, j8);
        continue;
      }
      int j9 = (j8 + jinc) % jmax;
      int j10 = (j9 + jinc) % jmax;

      // Factor 11.
      if (ifac == 11)
      {
        pfa11a(n1, z, mu, m, j0, j1, j2, j3, j4, j5, j6, j7, j8, j9, j10);
        continue;
      }
      int j11 = (j10 + jinc) % jmax;
      int j12 = (j11 + jinc) % jmax;

      // Factor 13.
      if (ifac == 13)
      {
        pfa13a(n1, z, mu, m, j0, j1, j2, j3, j4, j5, j6, j7, j8, j9, j10, j11, j12);
        continue;
      }
      int j13 = (j12 + jinc) % jmax;
      int j14 = (j13 + jinc) % jmax;
      int j15 = (j14 + jinc) % jmax;

      // Factor 16.
      if (ifac == 16)
      {
        pfa16a(n1, z, mu, m, j0, j1, j2, j3, j4, j5, j6, j7, j8, j9, j10, j11, j12, j13, j14, j15);
      }
    }
  }

  private static void pfa2a(int n1, double[][] z, int m, int j0, int j1)
  {
    int m1 = 2 * n1;
    for (int i = 0; i < m; ++i)
    {
      double[] zj0 = z[j0];
      double[] zj1 = z[j1];
      for (int i1 = 0; i1 < m1; i1 += 2)
      {
        double t1r = zj0[i1] - zj1[i1];
        double t1i = zj0[i1 + 1] - zj1[i1 + 1];
        zj0[i1] = zj0[i1] + zj1[i1];
        zj0[i1 + 1] = zj0[i1 + 1] + zj1[i1 + 1];
        zj1[i1] = t1r;
        zj1[i1 + 1] = t1i;
      }
      int jt = j1 + 1;
      j1 = j0 + 1;
      j0 = jt;
    }
  }

  private static void pfa3a(int n1, double[][] z, int mu, int m,
      int j0, int j1, int j2)
  {
    int m1 = 2 * n1;
    double c1;
    if (mu == 1)
    {
      c1 = P866;
    }
    else
    {
      c1 = -P866;
    }
    for (int i = 0; i < m; ++i)
    {
      double[] zj0 = z[j0];
      double[] zj1 = z[j1];
      double[] zj2 = z[j2];
      for (int i1 = 0; i1 < m1; i1 += 2)
      {
        double t1r = zj1[i1] + zj2[i1];
        double t1i = zj1[i1 + 1] + zj2[i1 + 1];
        double y1r = zj0[i1] - 0.5f * t1r;
        double y1i = zj0[i1 + 1] - 0.5f * t1i;
        double y2r = c1 * (zj1[i1] - zj2[i1]);
        double y2i = c1 * (zj1[i1 + 1] - zj2[i1 + 1]);
        zj0[i1] = zj0[i1] + t1r;
        zj0[i1 + 1] = zj0[i1 + 1] + t1i;
        zj1[i1] = y1r - y2i;
        zj1[i1 + 1] = y1i + y2r;
        zj2[i1] = y1r + y2i;
        zj2[i1 + 1] = y1i - y2r;
      }
      int jt = j2 + 1;
      j2 = j1 + 1;
      j1 = j0 + 1;
      j0 = jt;
    }
  }

  private static void pfa4a(int n1, double[][] z, int mu, int m,
      int j0, int j1, int j2, int j3)
  {
    int m1 = 2 * n1;
    double c1;
    if (mu == 1)
    {
      c1 = PONE;
    }
    else
    {
      c1 = -PONE;
    }
    for (int i = 0; i < m; ++i)
    {
      double[] zj0 = z[j0];
      double[] zj1 = z[j1];
      double[] zj2 = z[j2];
      double[] zj3 = z[j3];
      for (int i1 = 0; i1 < m1; i1 += 2)
      {
        double t1r = zj0[i1] + zj2[i1];
        double t1i = zj0[i1 + 1] + zj2[i1 + 1];
        double t2r = zj1[i1] + zj3[i1];
        double t2i = zj1[i1 + 1] + zj3[i1 + 1];
        double y1r = zj0[i1] - zj2[i1];
        double y1i = zj0[i1 + 1] - zj2[i1 + 1];
        double y3r = c1 * (zj1[i1] - zj3[i1]);
        double y3i = c1 * (zj1[i1 + 1] - zj3[i1 + 1]);
        zj0[i1] = t1r + t2r;
        zj0[i1 + 1] = t1i + t2i;
        zj1[i1] = y1r - y3i;
        zj1[i1 + 1] = y1i + y3r;
        zj2[i1] = t1r - t2r;
        zj2[i1 + 1] = t1i - t2i;
        zj3[i1] = y1r + y3i;
        zj3[i1 + 1] = y1i - y3r;
      }
      int jt = j3 + 1;
      j3 = j2 + 1;
      j2 = j1 + 1;
      j1 = j0 + 1;
      j0 = jt;
    }
  }

  private static void pfa5a(int n1, double[][] z, int mu, int m,
      int j0, int j1, int j2, int j3, int j4)
  {
    int m1 = 2 * n1;
    double c1, c2, c3;
    if (mu == 1)
    {
      c1 = P559;
      c2 = P951;
      c3 = P587;
    }
    else if (mu == 2)
    {
      c1 = -P559;
      c2 = P587;
      c3 = -P951;
    }
    else if (mu == 3)
    {
      c1 = -P559;
      c2 = -P587;
      c3 = P951;
    }
    else
    {
      c1 = P559;
      c2 = -P951;
      c3 = -P587;
    }
    for (int i = 0; i < m; ++i)
    {
      double[] zj0 = z[j0];
      double[] zj1 = z[j1];
      double[] zj2 = z[j2];
      double[] zj3 = z[j3];
      double[] zj4 = z[j4];
      for (int i1 = 0; i1 < m1; i1 += 2)
      {
        double t1r = zj1[i1] + zj4[i1];
        double t1i = zj1[i1 + 1] + zj4[i1 + 1];
        double t2r = zj2[i1] + zj3[i1];
        double t2i = zj2[i1 + 1] + zj3[i1 + 1];
        double t3r = zj1[i1] - zj4[i1];
        double t3i = zj1[i1 + 1] - zj4[i1 + 1];
        double t4r = zj2[i1] - zj3[i1];
        double t4i = zj2[i1 + 1] - zj3[i1 + 1];
        double t5r = t1r + t2r;
        double t5i = t1i + t2i;
        double t6r = c1 * (t1r - t2r);
        double t6i = c1 * (t1i - t2i);
        double t7r = zj0[i1] - 0.25f * t5r;
        double t7i = zj0[i1 + 1] - 0.25f * t5i;
        double y1r = t7r + t6r;
        double y1i = t7i + t6i;
        double y2r = t7r - t6r;
        double y2i = t7i - t6i;
        double y3r = c3 * t3r - c2 * t4r;
        double y3i = c3 * t3i - c2 * t4i;
        double y4r = c2 * t3r + c3 * t4r;
        double y4i = c2 * t3i + c3 * t4i;
        zj0[i1] = zj0[i1] + t5r;
        zj0[i1 + 1] = zj0[i1 + 1] + t5i;
        zj1[i1] = y1r - y4i;
        zj1[i1 + 1] = y1i + y4r;
        zj2[i1] = y2r - y3i;
        zj2[i1 + 1] = y2i + y3r;
        zj3[i1] = y2r + y3i;
        zj3[i1 + 1] = y2i - y3r;
        zj4[i1] = y1r + y4i;
        zj4[i1 + 1] = y1i - y4r;
      }
      int jt = j4 + 1;
      j4 = j3 + 1;
      j3 = j2 + 1;
      j2 = j1 + 1;
      j1 = j0 + 1;
      j0 = jt;
    }
  }

  private static void pfa7a(int n1, double[][] z, int mu, int m,
      int j0, int j1, int j2, int j3, int j4, int j5, int j6)
  {
    int m1 = 2 * n1;
    double c1, c2, c3, c4, c5, c6;
    if (mu == 1)
    {
      c1 = P623;
      c2 = -P222;
      c3 = -P900;
      c4 = P781;
      c5 = P974;
      c6 = P433;
    }
    else if (mu == 2)
    {
      c1 = -P222;
      c2 = -P900;
      c3 = P623;
      c4 = P974;
      c5 = -P433;
      c6 = -P781;
    }
    else if (mu == 3)
    {
      c1 = -P900;
      c2 = P623;
      c3 = -P222;
      c4 = P433;
      c5 = -P781;
      c6 = P974;
    }
    else if (mu == 4)
    {
      c1 = -P900;
      c2 = P623;
      c3 = -P222;
      c4 = -P433;
      c5 = P781;
      c6 = -P974;
    }
    else if (mu == 5)
    {
      c1 = -P222;
      c2 = -P900;
      c3 = P623;
      c4 = -P974;
      c5 = P433;
      c6 = P781;
    }
    else
    {
      c1 = P623;
      c2 = -P222;
      c3 = -P900;
      c4 = -P781;
      c5 = -P974;
      c6 = -P433;
    }
    for (int i = 0; i < m; ++i)
    {
      double[] zj0 = z[j0];
      double[] zj1 = z[j1];
      double[] zj2 = z[j2];
      double[] zj3 = z[j3];
      double[] zj4 = z[j4];
      double[] zj5 = z[j5];
      double[] zj6 = z[j6];
      for (int i1 = 0; i1 < m1; i1 += 2)
      {
        double t1r = zj1[i1] + zj6[i1];
        double t1i = zj1[i1 + 1] + zj6[i1 + 1];
        double t2r = zj2[i1] + zj5[i1];
        double t2i = zj2[i1 + 1] + zj5[i1 + 1];
        double t3r = zj3[i1] + zj4[i1];
        double t3i = zj3[i1 + 1] + zj4[i1 + 1];
        double t4r = zj1[i1] - zj6[i1];
        double t4i = zj1[i1 + 1] - zj6[i1 + 1];
        double t5r = zj2[i1] - zj5[i1];
        double t5i = zj2[i1 + 1] - zj5[i1 + 1];
        double t6r = zj3[i1] - zj4[i1];
        double t6i = zj3[i1 + 1] - zj4[i1 + 1];
        double t7r = zj0[i1] - 0.5f * t3r;
        double t7i = zj0[i1 + 1] - 0.5f * t3i;
        double t8r = t1r - t3r;
        double t8i = t1i - t3i;
        double t9r = t2r - t3r;
        double t9i = t2i - t3i;
        double y1r = t7r + c1 * t8r + c2 * t9r;
        double y1i = t7i + c1 * t8i + c2 * t9i;
        double y2r = t7r + c2 * t8r + c3 * t9r;
        double y2i = t7i + c2 * t8i + c3 * t9i;
        double y3r = t7r + c3 * t8r + c1 * t9r;
        double y3i = t7i + c3 * t8i + c1 * t9i;
        double y4r = c6 * t4r - c4 * t5r + c5 * t6r;
        double y4i = c6 * t4i - c4 * t5i + c5 * t6i;
        double y5r = c5 * t4r - c6 * t5r - c4 * t6r;
        double y5i = c5 * t4i - c6 * t5i - c4 * t6i;
        double y6r = c4 * t4r + c5 * t5r + c6 * t6r;
        double y6i = c4 * t4i + c5 * t5i + c6 * t6i;
        zj0[i1] = zj0[i1] + t1r + t2r + t3r;
        zj0[i1 + 1] = zj0[i1 + 1] + t1i + t2i + t3i;
        zj1[i1] = y1r - y6i;
        zj1[i1 + 1] = y1i + y6r;
        zj2[i1] = y2r - y5i;
        zj2[i1 + 1] = y2i + y5r;
        zj3[i1] = y3r - y4i;
        zj3[i1 + 1] = y3i + y4r;
        zj4[i1] = y3r + y4i;
        zj4[i1 + 1] = y3i - y4r;
        zj5[i1] = y2r + y5i;
        zj5[i1 + 1] = y2i - y5r;
        zj6[i1] = y1r + y6i;
        zj6[i1 + 1] = y1i - y6r;
      }
      int jt = j6 + 1;
      j6 = j5 + 1;
      j5 = j4 + 1;
      j4 = j3 + 1;
      j3 = j2 + 1;
      j2 = j1 + 1;
      j1 = j0 + 1;
      j0 = jt;
    }
  }

  private static void pfa8a(int n1, double[][] z, int mu, int m,
      int j0, int j1, int j2, int j3, int j4, int j5, int j6, int j7)
  {
    int m1 = 2 * n1;
    double c1, c2, c3;
    if (mu == 1)
    {
      c1 = PONE;
      c2 = P707;
    }
    else if (mu == 3)
    {
      c1 = -PONE;
      c2 = -P707;
    }
    else if (mu == 5)
    {
      c1 = PONE;
      c2 = -P707;
    }
    else
    {
      c1 = -PONE;
      c2 = P707;
    }
    c3 = c1 * c2;
    for (int i = 0; i < m; ++i)
    {
      double[] zj0 = z[j0];
      double[] zj1 = z[j1];
      double[] zj2 = z[j2];
      double[] zj3 = z[j3];
      double[] zj4 = z[j4];
      double[] zj5 = z[j5];
      double[] zj6 = z[j6];
      double[] zj7 = z[j7];
      for (int i1 = 0; i1 < m1; i1 += 2)
      {
        double t1r = zj0[i1] + zj4[i1];
        double t1i = zj0[i1 + 1] + zj4[i1 + 1];
        double t2r = zj0[i1] - zj4[i1];
        double t2i = zj0[i1 + 1] - zj4[i1 + 1];
        double t3r = zj1[i1] + zj5[i1];
        double t3i = zj1[i1 + 1] + zj5[i1 + 1];
        double t4r = zj1[i1] - zj5[i1];
        double t4i = zj1[i1 + 1] - zj5[i1 + 1];
        double t5r = zj2[i1] + zj6[i1];
        double t5i = zj2[i1 + 1] + zj6[i1 + 1];
        double t6r = c1 * (zj2[i1] - zj6[i1]);
        double t6i = c1 * (zj2[i1 + 1] - zj6[i1 + 1]);
        double t7r = zj3[i1] + zj7[i1];
        double t7i = zj3[i1 + 1] + zj7[i1 + 1];
        double t8r = zj3[i1] - zj7[i1];
        double t8i = zj3[i1 + 1] - zj7[i1 + 1];
        double t9r = t1r + t5r;
        double t9i = t1i + t5i;
        double t10r = t3r + t7r;
        double t10i = t3i + t7i;
        double t11r = c2 * (t4r - t8r);
        double t11i = c2 * (t4i - t8i);
        double t12r = c3 * (t4r + t8r);
        double t12i = c3 * (t4i + t8i);
        double y1r = t2r + t11r;
        double y1i = t2i + t11i;
        double y2r = t1r - t5r;
        double y2i = t1i - t5i;
        double y3r = t2r - t11r;
        double y3i = t2i - t11i;
        double y5r = t12r - t6r;
        double y5i = t12i - t6i;
        double y6r = c1 * (t3r - t7r);
        double y6i = c1 * (t3i - t7i);
        double y7r = t12r + t6r;
        double y7i = t12i + t6i;
        zj0[i1] = t9r + t10r;
        zj0[i1 + 1] = t9i + t10i;
        zj1[i1] = y1r - y7i;
        zj1[i1 + 1] = y1i + y7r;
        zj2[i1] = y2r - y6i;
        zj2[i1 + 1] = y2i + y6r;
        zj3[i1] = y3r - y5i;
        zj3[i1 + 1] = y3i + y5r;
        zj4[i1] = t9r - t10r;
        zj4[i1 + 1] = t9i - t10i;
        zj5[i1] = y3r + y5i;
        zj5[i1 + 1] = y3i - y5r;
        zj6[i1] = y2r + y6i;
        zj6[i1 + 1] = y2i - y6r;
        zj7[i1] = y1r + y7i;
        zj7[i1 + 1] = y1i - y7r;
      }
      int jt = j7 + 1;
      j7 = j6 + 1;
      j6 = j5 + 1;
      j5 = j4 + 1;
      j4 = j3 + 1;
      j3 = j2 + 1;
      j2 = j1 + 1;
      j1 = j0 + 1;
      j0 = jt;
    }
  }

  private static void pfa9a(int n1, double[][] z, int mu, int m,
      int j0, int j1, int j2, int j3, int j4, int j5, int j6, int j7, int j8)
  {
    int m1 = 2 * n1;
    double c1, c2, c3, c4, c5, c6, c7, c8, c9;
    if (mu == 1)
    {
      c1 = P866;
      c2 = P766;
      c3 = P642;
      c4 = P173;
      c5 = P984;
    }
    else if (mu == 2)
    {
      c1 = -P866;
      c2 = P173;
      c3 = P984;
      c4 = -P939;
      c5 = P342;
    }
    else if (mu == 4)
    {
      c1 = P866;
      c2 = -P939;
      c3 = P342;
      c4 = P766;
      c5 = -P642;
    }
    else if (mu == 5)
    {
      c1 = -P866;
      c2 = -P939;
      c3 = -P342;
      c4 = P766;
      c5 = P642;
    }
    else if (mu == 7)
    {
      c1 = P866;
      c2 = P173;
      c3 = -P984;
      c4 = -P939;
      c5 = -P342;
    }
    else
    {
      c1 = -P866;
      c2 = P766;
      c3 = -P642;
      c4 = P173;
      c5 = -P984;
    }
    c6 = c1 * c2;
    c7 = c1 * c3;
    c8 = c1 * c4;
    c9 = c1 * c5;
    for (int i = 0; i < m; ++i)
    {
      double[] zj0 = z[j0];
      double[] zj1 = z[j1];
      double[] zj2 = z[j2];
      double[] zj3 = z[j3];
      double[] zj4 = z[j4];
      double[] zj5 = z[j5];
      double[] zj6 = z[j6];
      double[] zj7 = z[j7];
      double[] zj8 = z[j8];
      for (int i1 = 0; i1 < m1; i1 += 2)
      {
        double t1r = zj3[i1] + zj6[i1];
        double t1i = zj3[i1 + 1] + zj6[i1 + 1];
        double t2r = zj0[i1] - 0.5f * t1r;
        double t2i = zj0[i1 + 1] - 0.5f * t1i;
        double t3r = c1 * (zj3[i1] - zj6[i1]);
        double t3i = c1 * (zj3[i1 + 1] - zj6[i1 + 1]);
        double t4r = zj0[i1] + t1r;
        double t4i = zj0[i1 + 1] + t1i;
        double t5r = zj4[i1] + zj7[i1];
        double t5i = zj4[i1 + 1] + zj7[i1 + 1];
        double t6r = zj1[i1] - 0.5f * t5r;
        double t6i = zj1[i1 + 1] - 0.5f * t5i;
        double t7r = zj4[i1] - zj7[i1];
        double t7i = zj4[i1 + 1] - zj7[i1 + 1];
        double t8r = zj1[i1] + t5r;
        double t8i = zj1[i1 + 1] + t5i;
        double t9r = zj2[i1] + zj5[i1];
        double t9i = zj2[i1 + 1] + zj5[i1 + 1];
        double t10r = zj8[i1] - 0.5f * t9r;
        double t10i = zj8[i1 + 1] - 0.5f * t9i;
        double t11r = zj2[i1] - zj5[i1];
        double t11i = zj2[i1 + 1] - zj5[i1 + 1];
        double t12r = zj8[i1] + t9r;
        double t12i = zj8[i1 + 1] + t9i;
        double t13r = t8r + t12r;
        double t13i = t8i + t12i;
        double t14r = t6r + t10r;
        double t14i = t6i + t10i;
        double t15r = t6r - t10r;
        double t15i = t6i - t10i;
        double t16r = t7r + t11r;
        double t16i = t7i + t11i;
        double t17r = t7r - t11r;
        double t17i = t7i - t11i;
        double t18r = c2 * t14r - c7 * t17r;
        double t18i = c2 * t14i - c7 * t17i;
        double t19r = c4 * t14r + c9 * t17r;
        double t19i = c4 * t14i + c9 * t17i;
        double t20r = c3 * t15r + c6 * t16r;
        double t20i = c3 * t15i + c6 * t16i;
        double t21r = c5 * t15r - c8 * t16r;
        double t21i = c5 * t15i - c8 * t16i;
        double t22r = t18r + t19r;
        double t22i = t18i + t19i;
        double t23r = t20r - t21r;
        double t23i = t20i - t21i;
        double y1r = t2r + t18r;
        double y1i = t2i + t18i;
        double y2r = t2r + t19r;
        double y2i = t2i + t19i;
        double y3r = t4r - 0.5f * t13r;
        double y3i = t4i - 0.5f * t13i;
        double y4r = t2r - t22r;
        double y4i = t2i - t22i;
        double y5r = t3r - t23r;
        double y5i = t3i - t23i;
        double y6r = c1 * (t8r - t12r);
        double y6i = c1 * (t8i - t12i);
        double y7r = t21r - t3r;
        double y7i = t21i - t3i;
        double y8r = t3r + t20r;
        double y8i = t3i + t20i;
        zj0[i1] = t4r + t13r;
        zj0[i1 + 1] = t4i + t13i;
        zj1[i1] = y1r - y8i;
        zj1[i1 + 1] = y1i + y8r;
        zj2[i1] = y2r - y7i;
        zj2[i1 + 1] = y2i + y7r;
        zj3[i1] = y3r - y6i;
        zj3[i1 + 1] = y3i + y6r;
        zj4[i1] = y4r - y5i;
        zj4[i1 + 1] = y4i + y5r;
        zj5[i1] = y4r + y5i;
        zj5[i1 + 1] = y4i - y5r;
        zj6[i1] = y3r + y6i;
        zj6[i1 + 1] = y3i - y6r;
        zj7[i1] = y2r + y7i;
        zj7[i1 + 1] = y2i - y7r;
        zj8[i1] = y1r + y8i;
        zj8[i1 + 1] = y1i - y8r;
      }
      int jt = j8 + 1;
      j8 = j7 + 1;
      j7 = j6 + 1;
      j6 = j5 + 1;
      j5 = j4 + 1;
      j4 = j3 + 1;
      j3 = j2 + 1;
      j2 = j1 + 1;
      j1 = j0 + 1;
      j0 = jt;
    }
  }

  private static void pfa11a(int n1, double[][] z, int mu, int m,
      int j0, int j1, int j2, int j3, int j4, int j5,
      int j6, int j7, int j8, int j9, int j10)
  {
    int m1 = 2 * n1;
    double c1, c2, c3, c4, c5, c6, c7, c8, c9, c10;
    if (mu == 1)
    {
      c1 = P841;
      c2 = P415;
      c3 = -P142;
      c4 = -P654;
      c5 = -P959;
      c6 = P540;
      c7 = P909;
      c8 = P989;
      c9 = P755;
      c10 = P281;
    }
    else if (mu == 2)
    {
      c1 = P415;
      c2 = -P654;
      c3 = -P959;
      c4 = -P142;
      c5 = P841;
      c6 = P909;
      c7 = P755;
      c8 = -P281;
      c9 = -P989;
      c10 = -P540;
    }
    else if (mu == 3)
    {
      c1 = -P142;
      c2 = -P959;
      c3 = P415;
      c4 = P841;
      c5 = -P654;
      c6 = P989;
      c7 = -P281;
      c8 = -P909;
      c9 = P540;
      c10 = P755;
    }
    else if (mu == 4)
    {
      c1 = -P654;
      c2 = -P142;
      c3 = P841;
      c4 = -P959;
      c5 = P415;
      c6 = P755;
      c7 = -P989;
      c8 = P540;
      c9 = P281;
      c10 = -P909;
    }
    else if (mu == 5)
    {
      c1 = -P959;
      c2 = P841;
      c3 = -P654;
      c4 = P415;
      c5 = -P142;
      c6 = P281;
      c7 = -P540;
      c8 = P755;
      c9 = -P909;
      c10 = P989;
    }
    else if (mu == 6)
    {
      c1 = -P959;
      c2 = P841;
      c3 = -P654;
      c4 = P415;
      c5 = -P142;
      c6 = -P281;
      c7 = P540;
      c8 = -P755;
      c9 = P909;
      c10 = -P989;
    }
    else if (mu == 7)
    {
      c1 = -P654;
      c2 = -P142;
      c3 = P841;
      c4 = -P959;
      c5 = P415;
      c6 = -P755;
      c7 = P989;
      c8 = -P540;
      c9 = -P281;
      c10 = P909;
    }
    else if (mu == 8)
    {
      c1 = -P142;
      c2 = -P959;
      c3 = P415;
      c4 = P841;
      c5 = -P654;
      c6 = -P989;
      c7 = P281;
      c8 = P909;
      c9 = -P540;
      c10 = -P755;
    }
    else if (mu == 9)
    {
      c1 = P415;
      c2 = -P654;
      c3 = -P959;
      c4 = -P142;
      c5 = P841;
      c6 = -P909;
      c7 = -P755;
      c8 = P281;
      c9 = P989;
      c10 = P540;
    }
    else
    {
      c1 = P841;
      c2 = P415;
      c3 = -P142;
      c4 = -P654;
      c5 = -P959;
      c6 = -P540;
      c7 = -P909;
      c8 = -P989;
      c9 = -P755;
      c10 = -P281;
    }
    for (int i = 0; i < m; ++i)
    {
      double[] zj0 = z[j0];
      double[] zj1 = z[j1];
      double[] zj2 = z[j2];
      double[] zj3 = z[j3];
      double[] zj4 = z[j4];
      double[] zj5 = z[j5];
      double[] zj6 = z[j6];
      double[] zj7 = z[j7];
      double[] zj8 = z[j8];
      double[] zj9 = z[j9];
      double[] zj10 = z[j10];
      for (int i1 = 0; i1 < m1; i1 += 2)
      {
        double t1r = zj1[i1] + zj10[i1];
        double t1i = zj1[i1 + 1] + zj10[i1 + 1];
        double t2r = zj2[i1] + zj9[i1];
        double t2i = zj2[i1 + 1] + zj9[i1 + 1];
        double t3r = zj3[i1] + zj8[i1];
        double t3i = zj3[i1 + 1] + zj8[i1 + 1];
        double t4r = zj4[i1] + zj7[i1];
        double t4i = zj4[i1 + 1] + zj7[i1 + 1];
        double t5r = zj5[i1] + zj6[i1];
        double t5i = zj5[i1 + 1] + zj6[i1 + 1];
        double t6r = zj1[i1] - zj10[i1];
        double t6i = zj1[i1 + 1] - zj10[i1 + 1];
        double t7r = zj2[i1] - zj9[i1];
        double t7i = zj2[i1 + 1] - zj9[i1 + 1];
        double t8r = zj3[i1] - zj8[i1];
        double t8i = zj3[i1 + 1] - zj8[i1 + 1];
        double t9r = zj4[i1] - zj7[i1];
        double t9i = zj4[i1 + 1] - zj7[i1 + 1];
        double t10r = zj5[i1] - zj6[i1];
        double t10i = zj5[i1 + 1] - zj6[i1 + 1];
        double t11r = zj0[i1] - 0.5f * t5r;
        double t11i = zj0[i1 + 1] - 0.5f * t5i;
        double t12r = t1r - t5r;
        double t12i = t1i - t5i;
        double t13r = t2r - t5r;
        double t13i = t2i - t5i;
        double t14r = t3r - t5r;
        double t14i = t3i - t5i;
        double t15r = t4r - t5r;
        double t15i = t4i - t5i;
        double y1r = t11r + c1 * t12r + c2 * t13r + c3 * t14r + c4 * t15r;
        double y1i = t11i + c1 * t12i + c2 * t13i + c3 * t14i + c4 * t15i;
        double y2r = t11r + c2 * t12r + c4 * t13r + c5 * t14r + c3 * t15r;
        double y2i = t11i + c2 * t12i + c4 * t13i + c5 * t14i + c3 * t15i;
        double y3r = t11r + c3 * t12r + c5 * t13r + c2 * t14r + c1 * t15r;
        double y3i = t11i + c3 * t12i + c5 * t13i + c2 * t14i + c1 * t15i;
        double y4r = t11r + c4 * t12r + c3 * t13r + c1 * t14r + c5 * t15r;
        double y4i = t11i + c4 * t12i + c3 * t13i + c1 * t14i + c5 * t15i;
        double y5r = t11r + c5 * t12r + c1 * t13r + c4 * t14r + c2 * t15r;
        double y5i = t11i + c5 * t12i + c1 * t13i + c4 * t14i + c2 * t15i;
        double y6r = c10 * t6r - c6 * t7r + c9 * t8r - c7 * t9r + c8 * t10r;
        double y6i = c10 * t6i - c6 * t7i + c9 * t8i - c7 * t9i + c8 * t10i;
        double y7r = c9 * t6r - c8 * t7r + c6 * t8r + c10 * t9r - c7 * t10r;
        double y7i = c9 * t6i - c8 * t7i + c6 * t8i + c10 * t9i - c7 * t10i;
        double y8r = c8 * t6r - c10 * t7r - c7 * t8r + c6 * t9r + c9 * t10r;
        double y8i = c8 * t6i - c10 * t7i - c7 * t8i + c6 * t9i + c9 * t10i;
        double y9r = c7 * t6r + c9 * t7r - c10 * t8r - c8 * t9r - c6 * t10r;
        double y9i = c7 * t6i + c9 * t7i - c10 * t8i - c8 * t9i - c6 * t10i;
        double y10r = c6 * t6r + c7 * t7r + c8 * t8r + c9 * t9r + c10 * t10r;
        double y10i = c6 * t6i + c7 * t7i + c8 * t8i + c9 * t9i + c10 * t10i;
        zj0[i1] = zj0[i1] + t1r + t2r + t3r + t4r + t5r;
        zj0[i1 + 1] = zj0[i1 + 1] + t1i + t2i + t3i + t4i + t5i;
        zj1[i1] = y1r - y10i;
        zj1[i1 + 1] = y1i + y10r;
        zj2[i1] = y2r - y9i;
        zj2[i1 + 1] = y2i + y9r;
        zj3[i1] = y3r - y8i;
        zj3[i1 + 1] = y3i + y8r;
        zj4[i1] = y4r - y7i;
        zj4[i1 + 1] = y4i + y7r;
        zj5[i1] = y5r - y6i;
        zj5[i1 + 1] = y5i + y6r;
        zj6[i1] = y5r + y6i;
        zj6[i1 + 1] = y5i - y6r;
        zj7[i1] = y4r + y7i;
        zj7[i1 + 1] = y4i - y7r;
        zj8[i1] = y3r + y8i;
        zj8[i1 + 1] = y3i - y8r;
        zj9[i1] = y2r + y9i;
        zj9[i1 + 1] = y2i - y9r;
        zj10[i1] = y1r + y10i;
        zj10[i1 + 1] = y1i - y10r;
      }
      int jt = j10 + 1;
      j10 = j9 + 1;
      j9 = j8 + 1;
      j8 = j7 + 1;
      j7 = j6 + 1;
      j6 = j5 + 1;
      j5 = j4 + 1;
      j4 = j3 + 1;
      j3 = j2 + 1;
      j2 = j1 + 1;
      j1 = j0 + 1;
      j0 = jt;
    }
  }

  private static void pfa13a(int n1, double[][] z, int mu, int m,
      int j0, int j1, int j2, int j3, int j4, int j5, int j6,
      int j7, int j8, int j9, int j10, int j11, int j12)
  {
    int m1 = 2 * n1;
    double c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12;
    if (mu == 1)
    {
      c1 = P885;
      c2 = P568;
      c3 = P120;
      c4 = -P354;
      c5 = -P748;
      c6 = -P970;
      c7 = P464;
      c8 = P822;
      c9 = P992;
      c10 = P935;
      c11 = P663;
      c12 = P239;
    }
    else if (mu == 2)
    {
      c1 = P568;
      c2 = -P354;
      c3 = -P970;
      c4 = -P748;
      c5 = P120;
      c6 = P885;
      c7 = P822;
      c8 = P935;
      c9 = P239;
      c10 = -P663;
      c11 = -P992;
      c12 = -P464;
    }
    else if (mu == 3)
    {
      c1 = P120;
      c2 = -P970;
      c3 = -P354;
      c4 = P885;
      c5 = P568;
      c6 = -P748;
      c7 = P992;
      c8 = P239;
      c9 = -P935;
      c10 = -P464;
      c11 = P822;
      c12 = P663;
    }
    else if (mu == 4)
    {
      c1 = -P354;
      c2 = -P748;
      c3 = P885;
      c4 = P120;
      c5 = -P970;
      c6 = P568;
      c7 = P935;
      c8 = -P663;
      c9 = -P464;
      c10 = P992;
      c11 = -P239;
      c12 = -P822;
    }
    else if (mu == 5)
    {
      c1 = -P748;
      c2 = P120;
      c3 = P568;
      c4 = -P970;
      c5 = P885;
      c6 = -P354;
      c7 = P663;
      c8 = -P992;
      c9 = P822;
      c10 = -P239;
      c11 = -P464;
      c12 = P935;
    }
    else if (mu == 6)
    {
      c1 = -P970;
      c2 = P885;
      c3 = -P748;
      c4 = P568;
      c5 = -P354;
      c6 = P120;
      c7 = P239;
      c8 = -P464;
      c9 = P663;
      c10 = -P822;
      c11 = P935;
      c12 = -P992;
    }
    else if (mu == 7)
    {
      c1 = -P970;
      c2 = P885;
      c3 = -P748;
      c4 = P568;
      c5 = -P354;
      c6 = P120;
      c7 = -P239;
      c8 = P464;
      c9 = -P663;
      c10 = P822;
      c11 = -P935;
      c12 = P992;
    }
    else if (mu == 8)
    {
      c1 = -P748;
      c2 = P120;
      c3 = P568;
      c4 = -P970;
      c5 = P885;
      c6 = -P354;
      c7 = -P663;
      c8 = P992;
      c9 = -P822;
      c10 = P239;
      c11 = P464;
      c12 = -P935;
    }
    else if (mu == 9)
    {
      c1 = -P354;
      c2 = -P748;
      c3 = P885;
      c4 = P120;
      c5 = -P970;
      c6 = P568;
      c7 = -P935;
      c8 = P663;
      c9 = P464;
      c10 = -P992;
      c11 = P239;
      c12 = P822;
    }
    else if (mu == 10)
    {
      c1 = P120;
      c2 = -P970;
      c3 = -P354;
      c4 = P885;
      c5 = P568;
      c6 = -P748;
      c7 = -P992;
      c8 = -P239;
      c9 = P935;
      c10 = P464;
      c11 = -P822;
      c12 = -P663;
    }
    else if (mu == 11)
    {
      c1 = P568;
      c2 = -P354;
      c3 = -P970;
      c4 = -P748;
      c5 = P120;
      c6 = P885;
      c7 = -P822;
      c8 = -P935;
      c9 = -P239;
      c10 = P663;
      c11 = P992;
      c12 = P464;
    }
    else
    {
      c1 = P885;
      c2 = P568;
      c3 = P120;
      c4 = -P354;
      c5 = -P748;
      c6 = -P970;
      c7 = -P464;
      c8 = -P822;
      c9 = -P992;
      c10 = -P935;
      c11 = -P663;
      c12 = -P239;
    }
    for (int i = 0; i < m; ++i)
    {
      double[] zj0 = z[j0];
      double[] zj1 = z[j1];
      double[] zj2 = z[j2];
      double[] zj3 = z[j3];
      double[] zj4 = z[j4];
      double[] zj5 = z[j5];
      double[] zj6 = z[j6];
      double[] zj7 = z[j7];
      double[] zj8 = z[j8];
      double[] zj9 = z[j9];
      double[] zj10 = z[j10];
      double[] zj11 = z[j11];
      double[] zj12 = z[j12];
      for (int i1 = 0; i1 < m1; i1 += 2)
      {
        double t1r = zj1[i1] + zj12[i1];
        double t1i = zj1[i1 + 1] + zj12[i1 + 1];
        double t2r = zj2[i1] + zj11[i1];
        double t2i = zj2[i1 + 1] + zj11[i1 + 1];
        double t3r = zj3[i1] + zj10[i1];
        double t3i = zj3[i1 + 1] + zj10[i1 + 1];
        double t4r = zj4[i1] + zj9[i1];
        double t4i = zj4[i1 + 1] + zj9[i1 + 1];
        double t5r = zj5[i1] + zj8[i1];
        double t5i = zj5[i1 + 1] + zj8[i1 + 1];
        double t6r = zj6[i1] + zj7[i1];
        double t6i = zj6[i1 + 1] + zj7[i1 + 1];
        double t7r = zj1[i1] - zj12[i1];
        double t7i = zj1[i1 + 1] - zj12[i1 + 1];
        double t8r = zj2[i1] - zj11[i1];
        double t8i = zj2[i1 + 1] - zj11[i1 + 1];
        double t9r = zj3[i1] - zj10[i1];
        double t9i = zj3[i1 + 1] - zj10[i1 + 1];
        double t10r = zj4[i1] - zj9[i1];
        double t10i = zj4[i1 + 1] - zj9[i1 + 1];
        double t11r = zj5[i1] - zj8[i1];
        double t11i = zj5[i1 + 1] - zj8[i1 + 1];
        double t12r = zj6[i1] - zj7[i1];
        double t12i = zj6[i1 + 1] - zj7[i1 + 1];
        double t13r = zj0[i1] - 0.5f * t6r;
        double t13i = zj0[i1 + 1] - 0.5f * t6i;
        double t14r = t1r - t6r;
        double t14i = t1i - t6i;
        double t15r = t2r - t6r;
        double t15i = t2i - t6i;
        double t16r = t3r - t6r;
        double t16i = t3i - t6i;
        double t17r = t4r - t6r;
        double t17i = t4i - t6i;
        double t18r = t5r - t6r;
        double t18i = t5i - t6i;
        double y1r = t13r + c1 * t14r + c2 * t15r + c3 * t16r + c4 * t17r + c5 * t18r;
        double y1i = t13i + c1 * t14i + c2 * t15i + c3 * t16i + c4 * t17i + c5 * t18i;
        double y2r = t13r + c2 * t14r + c4 * t15r + c6 * t16r + c5 * t17r + c3 * t18r;
        double y2i = t13i + c2 * t14i + c4 * t15i + c6 * t16i + c5 * t17i + c3 * t18i;
        double y3r = t13r + c3 * t14r + c6 * t15r + c4 * t16r + c1 * t17r + c2 * t18r;
        double y3i = t13i + c3 * t14i + c6 * t15i + c4 * t16i + c1 * t17i + c2 * t18i;
        double y4r = t13r + c4 * t14r + c5 * t15r + c1 * t16r + c3 * t17r + c6 * t18r;
        double y4i = t13i + c4 * t14i + c5 * t15i + c1 * t16i + c3 * t17i + c6 * t18i;
        double y5r = t13r + c5 * t14r + c3 * t15r + c2 * t16r + c6 * t17r + c1 * t18r;
        double y5i = t13i + c5 * t14i + c3 * t15i + c2 * t16i + c6 * t17i + c1 * t18i;
        double y6r = t13r + c6 * t14r + c1 * t15r + c5 * t16r + c2 * t17r + c4 * t18r;
        double y6i = t13i + c6 * t14i + c1 * t15i + c5 * t16i + c2 * t17i + c4 * t18i;
        double y7r = c12 * t7r - c7 * t8r + c11 * t9r - c8 * t10r + c10 * t11r - c9 * t12r;
        double y7i = c12 * t7i - c7 * t8i + c11 * t9i - c8 * t10i + c10 * t11i - c9 * t12i;
        double y8r = c11 * t7r - c9 * t8r + c8 * t9r - c12 * t10r - c7 * t11r + c10 * t12r;
        double y8i = c11 * t7i - c9 * t8i + c8 * t9i - c12 * t10i - c7 * t11i + c10 * t12i;
        double y9r = c10 * t7r - c11 * t8r - c7 * t9r + c9 * t10r - c12 * t11r - c8 * t12r;
        double y9i = c10 * t7i - c11 * t8i - c7 * t9i + c9 * t10i - c12 * t11i - c8 * t12i;
        double y10r = c9 * t7r + c12 * t8r - c10 * t9r - c7 * t10r + c8 * t11r + c11 * t12r;
        double y10i = c9 * t7i + c12 * t8i - c10 * t9i - c7 * t10i + c8 * t11i + c11 * t12i;
        double y11r = c8 * t7r + c10 * t8r + c12 * t9r - c11 * t10r - c9 * t11r - c7 * t12r;
        double y11i = c8 * t7i + c10 * t8i + c12 * t9i - c11 * t10i - c9 * t11i - c7 * t12i;
        double y12r = c7 * t7r + c8 * t8r + c9 * t9r + c10 * t10r + c11 * t11r + c12 * t12r;
        double y12i = c7 * t7i + c8 * t8i + c9 * t9i + c10 * t10i + c11 * t11i + c12 * t12i;
        zj0[i1] = zj0[i1] + t1r + t2r + t3r + t4r + t5r + t6r;
        zj0[i1 + 1] = zj0[i1 + 1] + t1i + t2i + t3i + t4i + t5i + t6i;
        zj1[i1] = y1r - y12i;
        zj1[i1 + 1] = y1i + y12r;
        zj2[i1] = y2r - y11i;
        zj2[i1 + 1] = y2i + y11r;
        zj3[i1] = y3r - y10i;
        zj3[i1 + 1] = y3i + y10r;
        zj4[i1] = y4r - y9i;
        zj4[i1 + 1] = y4i + y9r;
        zj5[i1] = y5r - y8i;
        zj5[i1 + 1] = y5i + y8r;
        zj6[i1] = y6r - y7i;
        zj6[i1 + 1] = y6i + y7r;
        zj7[i1] = y6r + y7i;
        zj7[i1 + 1] = y6i - y7r;
        zj8[i1] = y5r + y8i;
        zj8[i1 + 1] = y5i - y8r;
        zj9[i1] = y4r + y9i;
        zj9[i1 + 1] = y4i - y9r;
        zj10[i1] = y3r + y10i;
        zj10[i1 + 1] = y3i - y10r;
        zj11[i1] = y2r + y11i;
        zj11[i1 + 1] = y2i - y11r;
        zj12[i1] = y1r + y12i;
        zj12[i1 + 1] = y1i - y12r;
      }
      int jt = j12 + 1;
      j12 = j11 + 1;
      j11 = j10 + 1;
      j10 = j9 + 1;
      j9 = j8 + 1;
      j8 = j7 + 1;
      j7 = j6 + 1;
      j6 = j5 + 1;
      j5 = j4 + 1;
      j4 = j3 + 1;
      j3 = j2 + 1;
      j2 = j1 + 1;
      j1 = j0 + 1;
      j0 = jt;
    }
  }

  private static void pfa16a(int n1, double[][] z, int mu, int m,
      int j0, int j1, int j2, int j3, int j4, int j5, int j6, int j7, int j8,
      int j9, int j10, int j11, int j12, int j13, int j14, int j15)
  {
    int m1 = 2 * n1;
    double c1, c2, c3, c4, c5, c6, c7;
    if (mu == 1)
    {
      c1 = PONE;
      c2 = P923;
      c3 = P382;
      c4 = P707;
    }
    else if (mu == 3)
    {
      c1 = -PONE;
      c2 = P382;
      c3 = P923;
      c4 = -P707;
    }
    else if (mu == 5)
    {
      c1 = PONE;
      c2 = -P382;
      c3 = P923;
      c4 = -P707;
    }
    else if (mu == 7)
    {
      c1 = -PONE;
      c2 = -P923;
      c3 = P382;
      c4 = P707;
    }
    else if (mu == 9)
    {
      c1 = PONE;
      c2 = -P923;
      c3 = -P382;
      c4 = P707;
    }
    else if (mu == 11)
    {
      c1 = -PONE;
      c2 = -P382;
      c3 = -P923;
      c4 = -P707;
    }
    else if (mu == 13)
    {
      c1 = PONE;
      c2 = P382;
      c3 = -P923;
      c4 = -P707;
    }
    else
    {
      c1 = -PONE;
      c2 = P923;
      c3 = -P382;
      c4 = P707;
    }
    c5 = c1 * c4;
    c6 = c1 * c3;
    c7 = c1 * c2;
    for (int i = 0; i < m; ++i)
    {
      double[] zj0 = z[j0];
      double[] zj1 = z[j1];
      double[] zj2 = z[j2];
      double[] zj3 = z[j3];
      double[] zj4 = z[j4];
      double[] zj5 = z[j5];
      double[] zj6 = z[j6];
      double[] zj7 = z[j7];
      double[] zj8 = z[j8];
      double[] zj9 = z[j9];
      double[] zj10 = z[j10];
      double[] zj11 = z[j11];
      double[] zj12 = z[j12];
      double[] zj13 = z[j13];
      double[] zj14 = z[j14];
      double[] zj15 = z[j15];
      for (int i1 = 0; i1 < m1; i1 += 2)
      {
        double t1r = zj0[i1] + zj8[i1];
        double t1i = zj0[i1 + 1] + zj8[i1 + 1];
        double t2r = zj4[i1] + zj12[i1];
        double t2i = zj4[i1 + 1] + zj12[i1 + 1];
        double t3r = zj0[i1] - zj8[i1];
        double t3i = zj0[i1 + 1] - zj8[i1 + 1];
        double t4r = c1 * (zj4[i1] - zj12[i1]);
        double t4i = c1 * (zj4[i1 + 1] - zj12[i1 + 1]);
        double t5r = t1r + t2r;
        double t5i = t1i + t2i;
        double t6r = t1r - t2r;
        double t6i = t1i - t2i;
        double t7r = zj1[i1] + zj9[i1];
        double t7i = zj1[i1 + 1] + zj9[i1 + 1];
        double t8r = zj5[i1] + zj13[i1];
        double t8i = zj5[i1 + 1] + zj13[i1 + 1];
        double t9r = zj1[i1] - zj9[i1];
        double t9i = zj1[i1 + 1] - zj9[i1 + 1];
        double t10r = zj5[i1] - zj13[i1];
        double t10i = zj5[i1 + 1] - zj13[i1 + 1];
        double t11r = t7r + t8r;
        double t11i = t7i + t8i;
        double t12r = t7r - t8r;
        double t12i = t7i - t8i;
        double t13r = zj2[i1] + zj10[i1];
        double t13i = zj2[i1 + 1] + zj10[i1 + 1];
        double t14r = zj6[i1] + zj14[i1];
        double t14i = zj6[i1 + 1] + zj14[i1 + 1];
        double t15r = zj2[i1] - zj10[i1];
        double t15i = zj2[i1 + 1] - zj10[i1 + 1];
        double t16r = zj6[i1] - zj14[i1];
        double t16i = zj6[i1 + 1] - zj14[i1 + 1];
        double t17r = t13r + t14r;
        double t17i = t13i + t14i;
        double t18r = c4 * (t15r - t16r);
        double t18i = c4 * (t15i - t16i);
        double t19r = c5 * (t15r + t16r);
        double t19i = c5 * (t15i + t16i);
        double t20r = c1 * (t13r - t14r);
        double t20i = c1 * (t13i - t14i);
        double t21r = zj3[i1] + zj11[i1];
        double t21i = zj3[i1 + 1] + zj11[i1 + 1];
        double t22r = zj7[i1] + zj15[i1];
        double t22i = zj7[i1 + 1] + zj15[i1 + 1];
        double t23r = zj3[i1] - zj11[i1];
        double t23i = zj3[i1 + 1] - zj11[i1 + 1];
        double t24r = zj7[i1] - zj15[i1];
        double t24i = zj7[i1 + 1] - zj15[i1 + 1];
        double t25r = t21r + t22r;
        double t25i = t21i + t22i;
        double t26r = t21r - t22r;
        double t26i = t21i - t22i;
        double t27r = t9r + t24r;
        double t27i = t9i + t24i;
        double t28r = t10r + t23r;
        double t28i = t10i + t23i;
        double t29r = t9r - t24r;
        double t29i = t9i - t24i;
        double t30r = t10r - t23r;
        double t30i = t10i - t23i;
        double t31r = t5r + t17r;
        double t31i = t5i + t17i;
        double t32r = t11r + t25r;
        double t32i = t11i + t25i;
        double t33r = t3r + t18r;
        double t33i = t3i + t18i;
        double t34r = c2 * t29r - c6 * t30r;
        double t34i = c2 * t29i - c6 * t30i;
        double t35r = t3r - t18r;
        double t35i = t3i - t18i;
        double t36r = c7 * t27r - c3 * t28r;
        double t36i = c7 * t27i - c3 * t28i;
        double t37r = t4r + t19r;
        double t37i = t4i + t19i;
        double t38r = c3 * t27r + c7 * t28r;
        double t38i = c3 * t27i + c7 * t28i;
        double t39r = t4r - t19r;
        double t39i = t4i - t19i;
        double t40r = c6 * t29r + c2 * t30r;
        double t40i = c6 * t29i + c2 * t30i;
        double t41r = c4 * (t12r - t26r);
        double t41i = c4 * (t12i - t26i);
        double t42r = c5 * (t12r + t26r);
        double t42i = c5 * (t12i + t26i);
        double y1r = t33r + t34r;
        double y1i = t33i + t34i;
        double y2r = t6r + t41r;
        double y2i = t6i + t41i;
        double y3r = t35r + t40r;
        double y3i = t35i + t40i;
        double y4r = t5r - t17r;
        double y4i = t5i - t17i;
        double y5r = t35r - t40r;
        double y5i = t35i - t40i;
        double y6r = t6r - t41r;
        double y6i = t6i - t41i;
        double y7r = t33r - t34r;
        double y7i = t33i - t34i;
        double y9r = t38r - t37r;
        double y9i = t38i - t37i;
        double y10r = t42r - t20r;
        double y10i = t42i - t20i;
        double y11r = t36r + t39r;
        double y11i = t36i + t39i;
        double y12r = c1 * (t11r - t25r);
        double y12i = c1 * (t11i - t25i);
        double y13r = t36r - t39r;
        double y13i = t36i - t39i;
        double y14r = t42r + t20r;
        double y14i = t42i + t20i;
        double y15r = t38r + t37r;
        double y15i = t38i + t37i;
        zj0[i1] = t31r + t32r;
        zj0[i1 + 1] = t31i + t32i;
        zj1[i1] = y1r - y15i;
        zj1[i1 + 1] = y1i + y15r;
        zj2[i1] = y2r - y14i;
        zj2[i1 + 1] = y2i + y14r;
        zj3[i1] = y3r - y13i;
        zj3[i1 + 1] = y3i + y13r;
        zj4[i1] = y4r - y12i;
        zj4[i1 + 1] = y4i + y12r;
        zj5[i1] = y5r - y11i;
        zj5[i1 + 1] = y5i + y11r;
        zj6[i1] = y6r - y10i;
        zj6[i1 + 1] = y6i + y10r;
        zj7[i1] = y7r - y9i;
        zj7[i1 + 1] = y7i + y9r;
        zj8[i1] = t31r - t32r;
        zj8[i1 + 1] = t31i - t32i;
        zj9[i1] = y7r + y9i;
        zj9[i1 + 1] = y7i - y9r;
        zj10[i1] = y6r + y10i;
        zj10[i1 + 1] = y6i - y10r;
        zj11[i1] = y5r + y11i;
        zj11[i1 + 1] = y5i - y11r;
        zj12[i1] = y4r + y12i;
        zj12[i1 + 1] = y4i - y12r;
        zj13[i1] = y3r + y13i;
        zj13[i1 + 1] = y3i - y13r;
        zj14[i1] = y2r + y14i;
        zj14[i1 + 1] = y2i - y14r;
        zj15[i1] = y1r + y15i;
        zj15[i1 + 1] = y1i - y15r;
      }
      int jt = j15 + 1;
      j15 = j14 + 1;
      j14 = j13 + 1;
      j13 = j12 + 1;
      j12 = j11 + 1;
      j11 = j10 + 1;
      j10 = j9 + 1;
      j9 = j8 + 1;
      j8 = j7 + 1;
      j7 = j6 + 1;
      j6 = j5 + 1;
      j5 = j4 + 1;
      j4 = j3 + 1;
      j3 = j2 + 1;
      j2 = j1 + 1;
      j1 = j0 + 1;
      j0 = jt;
    }
  }

  /**
   * Prime-factor complex-to-complex multiple FFT. Performs multiple
   * transforms across the 2nd (slowest) dimension of a 2-D array.
   * In this version, z[0,2,4,...]][0:n1-1] contains the real parts,
   * an z[1,3,5,...][0:n1-1] contains the imaginary parts.
   * @param sign the sign of the exponent in the Fourier transform.
   * @param n1 the number of transforms (fast dimension).
   * @param nfft the FFT length (slow dimension).
   * @param z array[nfft*2][n1] of nfft*n1 complex numbers.
   */
  static void transform2b(int sign, int n1, int nfft, double[][] z)
  {

    // What is left of n after dividing by factors.
    int nleft = nfft;

    // Loop over all possible factors, from largest to smallest.
    for (int jfac = 0; jfac < NFAC; ++jfac)
    {

      // Skip the current factor, if not a mutually prime factor of n
      int ifac = _kfac[jfac];
      int ndiv = nleft / ifac;
      if (ndiv * ifac != nleft)
      {
        continue;
      }

      // What is left of n (nleft), and n divided by the current factor (m).
      nleft = ndiv;
      int m = nfft / ifac;

      // Rotation factor mu and stride mm.
      int mu = 0;
      int mm = 0;
      for (int kfac = 1; kfac <= ifac && mm % ifac != 1; ++kfac)
      {
        mu = kfac;
        mm = kfac * m;
      }
      if (sign < 0)
      {
        mu = ifac - mu;
      }

      // Array stride, bound, and indices.
      int jinc = 2 * mm;
      int jmax = 2 * nfft;
      int j0 = 0;
      int j1 = j0 + jinc;

      // Factor 2.
      if (ifac == 2)
      {
        pfa2b(n1, z, m, j0, j1);
        continue;
      }
      int j2 = (j1 + jinc) % jmax;

      // Factor 3.
      if (ifac == 3)
      {
        pfa3b(n1, z, mu, m, j0, j1, j2);
        continue;
      }
      int j3 = (j2 + jinc) % jmax;

      // Factor 4.
      if (ifac == 4)
      {
        pfa4b(n1, z, mu, m, j0, j1, j2, j3);
        continue;
      }
      int j4 = (j3 + jinc) % jmax;

      // Factor 5.
      if (ifac == 5)
      {
        pfa5b(n1, z, mu, m, j0, j1, j2, j3, j4);
        continue;
      }
      int j5 = (j4 + jinc) % jmax;
      int j6 = (j5 + jinc) % jmax;

      // Factor 7.
      if (ifac == 7)
      {
        pfa7b(n1, z, mu, m, j0, j1, j2, j3, j4, j5, j6);
        continue;
      }
      int j7 = (j6 + jinc) % jmax;

      // Factor 8.
      if (ifac == 8)
      {
        pfa8b(n1, z, mu, m, j0, j1, j2, j3, j4, j5, j6, j7);
        continue;
      }
      int j8 = (j7 + jinc) % jmax;

      // Factor 9.
      if (ifac == 9)
      {
        pfa9b(n1, z, mu, m, j0, j1, j2, j3, j4, j5, j6, j7, j8);
        continue;
      }
      int j9 = (j8 + jinc) % jmax;
      int j10 = (j9 + jinc) % jmax;

      // Factor 11.
      if (ifac == 11)
      {
        pfa11b(n1, z, mu, m, j0, j1, j2, j3, j4, j5, j6, j7, j8, j9, j10);
        continue;
      }
      int j11 = (j10 + jinc) % jmax;
      int j12 = (j11 + jinc) % jmax;

      // Factor 13.
      if (ifac == 13)
      {
        pfa13b(n1, z, mu, m, j0, j1, j2, j3, j4, j5, j6, j7, j8, j9, j10, j11, j12);
        continue;
      }
      int j13 = (j12 + jinc) % jmax;
      int j14 = (j13 + jinc) % jmax;
      int j15 = (j14 + jinc) % jmax;

      // Factor 16.
      if (ifac == 16)
      {
        pfa16b(n1, z, mu, m, j0, j1, j2, j3, j4, j5, j6, j7, j8, j9, j10, j11, j12, j13, j14, j15);
      }
    }
  }

  private static void pfa2b(int n1, double[][] z, int m, int j0, int j1)
  {
    for (int i = 0; i < m; ++i)
    {
      double[] zj0r = z[j0];
      double[] zj0i = z[j0 + 1];
      double[] zj1r = z[j1];
      double[] zj1i = z[j1 + 1];
      for (int i1 = 0; i1 < n1; ++i1)
      {
        double t1r = zj0r[i1] - zj1r[i1];
        double t1i = zj0i[i1] - zj1i[i1];
        zj0r[i1] = zj0r[i1] + zj1r[i1];
        zj0i[i1] = zj0i[i1] + zj1i[i1];
        zj1r[i1] = t1r;
        zj1i[i1] = t1i;
      }
      int jt = j1 + 2;
      j1 = j0 + 2;
      j0 = jt;
    }
  }

  private static void pfa3b(int n1, double[][] z, int mu, int m,
      int j0, int j1, int j2)
  {
    double c1;
    if (mu == 1)
    {
      c1 = P866;
    }
    else
    {
      c1 = -P866;
    }
    for (int i = 0; i < m; ++i)
    {
      double[] zj0r = z[j0];
      double[] zj0i = z[j0 + 1];
      double[] zj1r = z[j1];
      double[] zj1i = z[j1 + 1];
      double[] zj2r = z[j2];
      double[] zj2i = z[j2 + 1];
      for (int i1 = 0; i1 < n1; ++i1)
      {
        double t1r = zj1r[i1] + zj2r[i1];
        double t1i = zj1i[i1] + zj2i[i1];
        double y1r = zj0r[i1] - 0.5f * t1r;
        double y1i = zj0i[i1] - 0.5f * t1i;
        double y2r = c1 * (zj1r[i1] - zj2r[i1]);
        double y2i = c1 * (zj1i[i1] - zj2i[i1]);
        zj0r[i1] = zj0r[i1] + t1r;
        zj0i[i1] = zj0i[i1] + t1i;
        zj1r[i1] = y1r - y2i;
        zj1i[i1] = y1i + y2r;
        zj2r[i1] = y1r + y2i;
        zj2i[i1] = y1i - y2r;
      }
      int jt = j2 + 2;
      j2 = j1 + 2;
      j1 = j0 + 2;
      j0 = jt;
    }
  }

  private static void pfa4b(int n1, double[][] z, int mu, int m,
      int j0, int j1, int j2, int j3)
  {
    double c1;
    if (mu == 1)
    {
      c1 = PONE;
    }
    else
    {
      c1 = -PONE;
    }
    for (int i = 0; i < m; ++i)
    {
      double[] zj0r = z[j0];
      double[] zj0i = z[j0 + 1];
      double[] zj1r = z[j1];
      double[] zj1i = z[j1 + 1];
      double[] zj2r = z[j2];
      double[] zj2i = z[j2 + 1];
      double[] zj3r = z[j3];
      double[] zj3i = z[j3 + 1];
      for (int i1 = 0; i1 < n1; ++i1)
      {
        double t1r = zj0r[i1] + zj2r[i1];
        double t1i = zj0i[i1] + zj2i[i1];
        double t2r = zj1r[i1] + zj3r[i1];
        double t2i = zj1i[i1] + zj3i[i1];
        double y1r = zj0r[i1] - zj2r[i1];
        double y1i = zj0i[i1] - zj2i[i1];
        double y3r = c1 * (zj1r[i1] - zj3r[i1]);
        double y3i = c1 * (zj1i[i1] - zj3i[i1]);
        zj0r[i1] = t1r + t2r;
        zj0i[i1] = t1i + t2i;
        zj1r[i1] = y1r - y3i;
        zj1i[i1] = y1i + y3r;
        zj2r[i1] = t1r - t2r;
        zj2i[i1] = t1i - t2i;
        zj3r[i1] = y1r + y3i;
        zj3i[i1] = y1i - y3r;
      }
      int jt = j3 + 2;
      j3 = j2 + 2;
      j2 = j1 + 2;
      j1 = j0 + 2;
      j0 = jt;
    }
  }

  private static void pfa5b(int n1, double[][] z, int mu, int m,
      int j0, int j1, int j2, int j3, int j4)
  {
    double c1, c2, c3;
    if (mu == 1)
    {
      c1 = P559;
      c2 = P951;
      c3 = P587;
    }
    else if (mu == 2)
    {
      c1 = -P559;
      c2 = P587;
      c3 = -P951;
    }
    else if (mu == 3)
    {
      c1 = -P559;
      c2 = -P587;
      c3 = P951;
    }
    else
    {
      c1 = P559;
      c2 = -P951;
      c3 = -P587;
    }
    for (int i = 0; i < m; ++i)
    {
      double[] zj0r = z[j0];
      double[] zj0i = z[j0 + 1];
      double[] zj1r = z[j1];
      double[] zj1i = z[j1 + 1];
      double[] zj2r = z[j2];
      double[] zj2i = z[j2 + 1];
      double[] zj3r = z[j3];
      double[] zj3i = z[j3 + 1];
      double[] zj4r = z[j4];
      double[] zj4i = z[j4 + 1];
      for (int i1 = 0; i1 < n1; ++i1)
      {
        double t1r = zj1r[i1] + zj4r[i1];
        double t1i = zj1i[i1] + zj4i[i1];
        double t2r = zj2r[i1] + zj3r[i1];
        double t2i = zj2i[i1] + zj3i[i1];
        double t3r = zj1r[i1] - zj4r[i1];
        double t3i = zj1i[i1] - zj4i[i1];
        double t4r = zj2r[i1] - zj3r[i1];
        double t4i = zj2i[i1] - zj3i[i1];
        double t5r = t1r + t2r;
        double t5i = t1i + t2i;
        double t6r = c1 * (t1r - t2r);
        double t6i = c1 * (t1i - t2i);
        double t7r = zj0r[i1] - 0.25f * t5r;
        double t7i = zj0i[i1] - 0.25f * t5i;
        double y1r = t7r + t6r;
        double y1i = t7i + t6i;
        double y2r = t7r - t6r;
        double y2i = t7i - t6i;
        double y3r = c3 * t3r - c2 * t4r;
        double y3i = c3 * t3i - c2 * t4i;
        double y4r = c2 * t3r + c3 * t4r;
        double y4i = c2 * t3i + c3 * t4i;
        zj0r[i1] = zj0r[i1] + t5r;
        zj0i[i1] = zj0i[i1] + t5i;
        zj1r[i1] = y1r - y4i;
        zj1i[i1] = y1i + y4r;
        zj2r[i1] = y2r - y3i;
        zj2i[i1] = y2i + y3r;
        zj3r[i1] = y2r + y3i;
        zj3i[i1] = y2i - y3r;
        zj4r[i1] = y1r + y4i;
        zj4i[i1] = y1i - y4r;
      }
      int jt = j4 + 2;
      j4 = j3 + 2;
      j3 = j2 + 2;
      j2 = j1 + 2;
      j1 = j0 + 2;
      j0 = jt;
    }
  }

  private static void pfa7b(int n1, double[][] z, int mu, int m,
      int j0, int j1, int j2, int j3, int j4, int j5, int j6)
  {
    double c1, c2, c3, c4, c5, c6;
    if (mu == 1)
    {
      c1 = P623;
      c2 = -P222;
      c3 = -P900;
      c4 = P781;
      c5 = P974;
      c6 = P433;
    }
    else if (mu == 2)
    {
      c1 = -P222;
      c2 = -P900;
      c3 = P623;
      c4 = P974;
      c5 = -P433;
      c6 = -P781;
    }
    else if (mu == 3)
    {
      c1 = -P900;
      c2 = P623;
      c3 = -P222;
      c4 = P433;
      c5 = -P781;
      c6 = P974;
    }
    else if (mu == 4)
    {
      c1 = -P900;
      c2 = P623;
      c3 = -P222;
      c4 = -P433;
      c5 = P781;
      c6 = -P974;
    }
    else if (mu == 5)
    {
      c1 = -P222;
      c2 = -P900;
      c3 = P623;
      c4 = -P974;
      c5 = P433;
      c6 = P781;
    }
    else
    {
      c1 = P623;
      c2 = -P222;
      c3 = -P900;
      c4 = -P781;
      c5 = -P974;
      c6 = -P433;
    }
    for (int i = 0; i < m; ++i)
    {
      double[] zj0r = z[j0];
      double[] zj0i = z[j0 + 1];
      double[] zj1r = z[j1];
      double[] zj1i = z[j1 + 1];
      double[] zj2r = z[j2];
      double[] zj2i = z[j2 + 1];
      double[] zj3r = z[j3];
      double[] zj3i = z[j3 + 1];
      double[] zj4r = z[j4];
      double[] zj4i = z[j4 + 1];
      double[] zj5r = z[j5];
      double[] zj5i = z[j5 + 1];
      double[] zj6r = z[j6];
      double[] zj6i = z[j6 + 1];
      for (int i1 = 0; i1 < n1; ++i1)
      {
        double t1r = zj1r[i1] + zj6r[i1];
        double t1i = zj1i[i1] + zj6i[i1];
        double t2r = zj2r[i1] + zj5r[i1];
        double t2i = zj2i[i1] + zj5i[i1];
        double t3r = zj3r[i1] + zj4r[i1];
        double t3i = zj3i[i1] + zj4i[i1];
        double t4r = zj1r[i1] - zj6r[i1];
        double t4i = zj1i[i1] - zj6i[i1];
        double t5r = zj2r[i1] - zj5r[i1];
        double t5i = zj2i[i1] - zj5i[i1];
        double t6r = zj3r[i1] - zj4r[i1];
        double t6i = zj3i[i1] - zj4i[i1];
        double t7r = zj0r[i1] - 0.5f * t3r;
        double t7i = zj0i[i1] - 0.5f * t3i;
        double t8r = t1r - t3r;
        double t8i = t1i - t3i;
        double t9r = t2r - t3r;
        double t9i = t2i - t3i;
        double y1r = t7r + c1 * t8r + c2 * t9r;
        double y1i = t7i + c1 * t8i + c2 * t9i;
        double y2r = t7r + c2 * t8r + c3 * t9r;
        double y2i = t7i + c2 * t8i + c3 * t9i;
        double y3r = t7r + c3 * t8r + c1 * t9r;
        double y3i = t7i + c3 * t8i + c1 * t9i;
        double y4r = c6 * t4r - c4 * t5r + c5 * t6r;
        double y4i = c6 * t4i - c4 * t5i + c5 * t6i;
        double y5r = c5 * t4r - c6 * t5r - c4 * t6r;
        double y5i = c5 * t4i - c6 * t5i - c4 * t6i;
        double y6r = c4 * t4r + c5 * t5r + c6 * t6r;
        double y6i = c4 * t4i + c5 * t5i + c6 * t6i;
        zj0r[i1] = zj0r[i1] + t1r + t2r + t3r;
        zj0i[i1] = zj0i[i1] + t1i + t2i + t3i;
        zj1r[i1] = y1r - y6i;
        zj1i[i1] = y1i + y6r;
        zj2r[i1] = y2r - y5i;
        zj2i[i1] = y2i + y5r;
        zj3r[i1] = y3r - y4i;
        zj3i[i1] = y3i + y4r;
        zj4r[i1] = y3r + y4i;
        zj4i[i1] = y3i - y4r;
        zj5r[i1] = y2r + y5i;
        zj5i[i1] = y2i - y5r;
        zj6r[i1] = y1r + y6i;
        zj6i[i1] = y1i - y6r;
      }
      int jt = j6 + 2;
      j6 = j5 + 2;
      j5 = j4 + 2;
      j4 = j3 + 2;
      j3 = j2 + 2;
      j2 = j1 + 2;
      j1 = j0 + 2;
      j0 = jt;
    }
  }

  private static void pfa8b(int n1, double[][] z, int mu, int m,
      int j0, int j1, int j2, int j3, int j4, int j5, int j6, int j7)
  {
    double c1, c2, c3;
    if (mu == 1)
    {
      c1 = PONE;
      c2 = P707;
    }
    else if (mu == 3)
    {
      c1 = -PONE;
      c2 = -P707;
    }
    else if (mu == 5)
    {
      c1 = PONE;
      c2 = -P707;
    }
    else
    {
      c1 = -PONE;
      c2 = P707;
    }
    c3 = c1 * c2;
    for (int i = 0; i < m; ++i)
    {
      double[] zj0r = z[j0];
      double[] zj0i = z[j0 + 1];
      double[] zj1r = z[j1];
      double[] zj1i = z[j1 + 1];
      double[] zj2r = z[j2];
      double[] zj2i = z[j2 + 1];
      double[] zj3r = z[j3];
      double[] zj3i = z[j3 + 1];
      double[] zj4r = z[j4];
      double[] zj4i = z[j4 + 1];
      double[] zj5r = z[j5];
      double[] zj5i = z[j5 + 1];
      double[] zj6r = z[j6];
      double[] zj6i = z[j6 + 1];
      double[] zj7r = z[j7];
      double[] zj7i = z[j7 + 1];
      for (int i1 = 0; i1 < n1; ++i1)
      {
        double t1r = zj0r[i1] + zj4r[i1];
        double t1i = zj0i[i1] + zj4i[i1];
        double t2r = zj0r[i1] - zj4r[i1];
        double t2i = zj0i[i1] - zj4i[i1];
        double t3r = zj1r[i1] + zj5r[i1];
        double t3i = zj1i[i1] + zj5i[i1];
        double t4r = zj1r[i1] - zj5r[i1];
        double t4i = zj1i[i1] - zj5i[i1];
        double t5r = zj2r[i1] + zj6r[i1];
        double t5i = zj2i[i1] + zj6i[i1];
        double t6r = c1 * (zj2r[i1] - zj6r[i1]);
        double t6i = c1 * (zj2i[i1] - zj6i[i1]);
        double t7r = zj3r[i1] + zj7r[i1];
        double t7i = zj3i[i1] + zj7i[i1];
        double t8r = zj3r[i1] - zj7r[i1];
        double t8i = zj3i[i1] - zj7i[i1];
        double t9r = t1r + t5r;
        double t9i = t1i + t5i;
        double t10r = t3r + t7r;
        double t10i = t3i + t7i;
        double t11r = c2 * (t4r - t8r);
        double t11i = c2 * (t4i - t8i);
        double t12r = c3 * (t4r + t8r);
        double t12i = c3 * (t4i + t8i);
        double y1r = t2r + t11r;
        double y1i = t2i + t11i;
        double y2r = t1r - t5r;
        double y2i = t1i - t5i;
        double y3r = t2r - t11r;
        double y3i = t2i - t11i;
        double y5r = t12r - t6r;
        double y5i = t12i - t6i;
        double y6r = c1 * (t3r - t7r);
        double y6i = c1 * (t3i - t7i);
        double y7r = t12r + t6r;
        double y7i = t12i + t6i;
        zj0r[i1] = t9r + t10r;
        zj0i[i1] = t9i + t10i;
        zj1r[i1] = y1r - y7i;
        zj1i[i1] = y1i + y7r;
        zj2r[i1] = y2r - y6i;
        zj2i[i1] = y2i + y6r;
        zj3r[i1] = y3r - y5i;
        zj3i[i1] = y3i + y5r;
        zj4r[i1] = t9r - t10r;
        zj4i[i1] = t9i - t10i;
        zj5r[i1] = y3r + y5i;
        zj5i[i1] = y3i - y5r;
        zj6r[i1] = y2r + y6i;
        zj6i[i1] = y2i - y6r;
        zj7r[i1] = y1r + y7i;
        zj7i[i1] = y1i - y7r;
      }
      int jt = j7 + 2;
      j7 = j6 + 2;
      j6 = j5 + 2;
      j5 = j4 + 2;
      j4 = j3 + 2;
      j3 = j2 + 2;
      j2 = j1 + 2;
      j1 = j0 + 2;
      j0 = jt;
    }
  }

  private static void pfa9b(int n1, double[][] z, int mu, int m,
      int j0, int j1, int j2, int j3, int j4, int j5, int j6, int j7, int j8)
  {
    double c1, c2, c3, c4, c5, c6, c7, c8, c9;
    if (mu == 1)
    {
      c1 = P866;
      c2 = P766;
      c3 = P642;
      c4 = P173;
      c5 = P984;
    }
    else if (mu == 2)
    {
      c1 = -P866;
      c2 = P173;
      c3 = P984;
      c4 = -P939;
      c5 = P342;
    }
    else if (mu == 4)
    {
      c1 = P866;
      c2 = -P939;
      c3 = P342;
      c4 = P766;
      c5 = -P642;
    }
    else if (mu == 5)
    {
      c1 = -P866;
      c2 = -P939;
      c3 = -P342;
      c4 = P766;
      c5 = P642;
    }
    else if (mu == 7)
    {
      c1 = P866;
      c2 = P173;
      c3 = -P984;
      c4 = -P939;
      c5 = -P342;
    }
    else
    {
      c1 = -P866;
      c2 = P766;
      c3 = -P642;
      c4 = P173;
      c5 = -P984;
    }
    c6 = c1 * c2;
    c7 = c1 * c3;
    c8 = c1 * c4;
    c9 = c1 * c5;
    for (int i = 0; i < m; ++i)
    {
      double[] zj0r = z[j0];
      double[] zj0i = z[j0 + 1];
      double[] zj1r = z[j1];
      double[] zj1i = z[j1 + 1];
      double[] zj2r = z[j2];
      double[] zj2i = z[j2 + 1];
      double[] zj3r = z[j3];
      double[] zj3i = z[j3 + 1];
      double[] zj4r = z[j4];
      double[] zj4i = z[j4 + 1];
      double[] zj5r = z[j5];
      double[] zj5i = z[j5 + 1];
      double[] zj6r = z[j6];
      double[] zj6i = z[j6 + 1];
      double[] zj7r = z[j7];
      double[] zj7i = z[j7 + 1];
      double[] zj8r = z[j8];
      double[] zj8i = z[j8 + 1];
      for (int i1 = 0; i1 < n1; ++i1)
      {
        double t1r = zj3r[i1] + zj6r[i1];
        double t1i = zj3i[i1] + zj6i[i1];
        double t2r = zj0r[i1] - 0.5f * t1r;
        double t2i = zj0i[i1] - 0.5f * t1i;
        double t3r = c1 * (zj3r[i1] - zj6r[i1]);
        double t3i = c1 * (zj3i[i1] - zj6i[i1]);
        double t4r = zj0r[i1] + t1r;
        double t4i = zj0i[i1] + t1i;
        double t5r = zj4r[i1] + zj7r[i1];
        double t5i = zj4i[i1] + zj7i[i1];
        double t6r = zj1r[i1] - 0.5f * t5r;
        double t6i = zj1i[i1] - 0.5f * t5i;
        double t7r = zj4r[i1] - zj7r[i1];
        double t7i = zj4i[i1] - zj7i[i1];
        double t8r = zj1r[i1] + t5r;
        double t8i = zj1i[i1] + t5i;
        double t9r = zj2r[i1] + zj5r[i1];
        double t9i = zj2i[i1] + zj5i[i1];
        double t10r = zj8r[i1] - 0.5f * t9r;
        double t10i = zj8i[i1] - 0.5f * t9i;
        double t11r = zj2r[i1] - zj5r[i1];
        double t11i = zj2i[i1] - zj5i[i1];
        double t12r = zj8r[i1] + t9r;
        double t12i = zj8i[i1] + t9i;
        double t13r = t8r + t12r;
        double t13i = t8i + t12i;
        double t14r = t6r + t10r;
        double t14i = t6i + t10i;
        double t15r = t6r - t10r;
        double t15i = t6i - t10i;
        double t16r = t7r + t11r;
        double t16i = t7i + t11i;
        double t17r = t7r - t11r;
        double t17i = t7i - t11i;
        double t18r = c2 * t14r - c7 * t17r;
        double t18i = c2 * t14i - c7 * t17i;
        double t19r = c4 * t14r + c9 * t17r;
        double t19i = c4 * t14i + c9 * t17i;
        double t20r = c3 * t15r + c6 * t16r;
        double t20i = c3 * t15i + c6 * t16i;
        double t21r = c5 * t15r - c8 * t16r;
        double t21i = c5 * t15i - c8 * t16i;
        double t22r = t18r + t19r;
        double t22i = t18i + t19i;
        double t23r = t20r - t21r;
        double t23i = t20i - t21i;
        double y1r = t2r + t18r;
        double y1i = t2i + t18i;
        double y2r = t2r + t19r;
        double y2i = t2i + t19i;
        double y3r = t4r - 0.5f * t13r;
        double y3i = t4i - 0.5f * t13i;
        double y4r = t2r - t22r;
        double y4i = t2i - t22i;
        double y5r = t3r - t23r;
        double y5i = t3i - t23i;
        double y6r = c1 * (t8r - t12r);
        double y6i = c1 * (t8i - t12i);
        double y7r = t21r - t3r;
        double y7i = t21i - t3i;
        double y8r = t3r + t20r;
        double y8i = t3i + t20i;
        zj0r[i1] = t4r + t13r;
        zj0i[i1] = t4i + t13i;
        zj1r[i1] = y1r - y8i;
        zj1i[i1] = y1i + y8r;
        zj2r[i1] = y2r - y7i;
        zj2i[i1] = y2i + y7r;
        zj3r[i1] = y3r - y6i;
        zj3i[i1] = y3i + y6r;
        zj4r[i1] = y4r - y5i;
        zj4i[i1] = y4i + y5r;
        zj5r[i1] = y4r + y5i;
        zj5i[i1] = y4i - y5r;
        zj6r[i1] = y3r + y6i;
        zj6i[i1] = y3i - y6r;
        zj7r[i1] = y2r + y7i;
        zj7i[i1] = y2i - y7r;
        zj8r[i1] = y1r + y8i;
        zj8i[i1] = y1i - y8r;
      }
      int jt = j8 + 2;
      j8 = j7 + 2;
      j7 = j6 + 2;
      j6 = j5 + 2;
      j5 = j4 + 2;
      j4 = j3 + 2;
      j3 = j2 + 2;
      j2 = j1 + 2;
      j1 = j0 + 2;
      j0 = jt;
    }
  }

  private static void pfa11b(int n1, double[][] z, int mu, int m,
      int j0, int j1, int j2, int j3, int j4, int j5,
      int j6, int j7, int j8, int j9, int j10)
  {
    double c1, c2, c3, c4, c5, c6, c7, c8, c9, c10;
    if (mu == 1)
    {
      c1 = P841;
      c2 = P415;
      c3 = -P142;
      c4 = -P654;
      c5 = -P959;
      c6 = P540;
      c7 = P909;
      c8 = P989;
      c9 = P755;
      c10 = P281;
    }
    else if (mu == 2)
    {
      c1 = P415;
      c2 = -P654;
      c3 = -P959;
      c4 = -P142;
      c5 = P841;
      c6 = P909;
      c7 = P755;
      c8 = -P281;
      c9 = -P989;
      c10 = -P540;
    }
    else if (mu == 3)
    {
      c1 = -P142;
      c2 = -P959;
      c3 = P415;
      c4 = P841;
      c5 = -P654;
      c6 = P989;
      c7 = -P281;
      c8 = -P909;
      c9 = P540;
      c10 = P755;
    }
    else if (mu == 4)
    {
      c1 = -P654;
      c2 = -P142;
      c3 = P841;
      c4 = -P959;
      c5 = P415;
      c6 = P755;
      c7 = -P989;
      c8 = P540;
      c9 = P281;
      c10 = -P909;
    }
    else if (mu == 5)
    {
      c1 = -P959;
      c2 = P841;
      c3 = -P654;
      c4 = P415;
      c5 = -P142;
      c6 = P281;
      c7 = -P540;
      c8 = P755;
      c9 = -P909;
      c10 = P989;
    }
    else if (mu == 6)
    {
      c1 = -P959;
      c2 = P841;
      c3 = -P654;
      c4 = P415;
      c5 = -P142;
      c6 = -P281;
      c7 = P540;
      c8 = -P755;
      c9 = P909;
      c10 = -P989;
    }
    else if (mu == 7)
    {
      c1 = -P654;
      c2 = -P142;
      c3 = P841;
      c4 = -P959;
      c5 = P415;
      c6 = -P755;
      c7 = P989;
      c8 = -P540;
      c9 = -P281;
      c10 = P909;
    }
    else if (mu == 8)
    {
      c1 = -P142;
      c2 = -P959;
      c3 = P415;
      c4 = P841;
      c5 = -P654;
      c6 = -P989;
      c7 = P281;
      c8 = P909;
      c9 = -P540;
      c10 = -P755;
    }
    else if (mu == 9)
    {
      c1 = P415;
      c2 = -P654;
      c3 = -P959;
      c4 = -P142;
      c5 = P841;
      c6 = -P909;
      c7 = -P755;
      c8 = P281;
      c9 = P989;
      c10 = P540;
    }
    else
    {
      c1 = P841;
      c2 = P415;
      c3 = -P142;
      c4 = -P654;
      c5 = -P959;
      c6 = -P540;
      c7 = -P909;
      c8 = -P989;
      c9 = -P755;
      c10 = -P281;
    }
    for (int i = 0; i < m; ++i)
    {
      double[] zj0r = z[j0];
      double[] zj0i = z[j0 + 1];
      double[] zj1r = z[j1];
      double[] zj1i = z[j1 + 1];
      double[] zj2r = z[j2];
      double[] zj2i = z[j2 + 1];
      double[] zj3r = z[j3];
      double[] zj3i = z[j3 + 1];
      double[] zj4r = z[j4];
      double[] zj4i = z[j4 + 1];
      double[] zj5r = z[j5];
      double[] zj5i = z[j5 + 1];
      double[] zj6r = z[j6];
      double[] zj6i = z[j6 + 1];
      double[] zj7r = z[j7];
      double[] zj7i = z[j7 + 1];
      double[] zj8r = z[j8];
      double[] zj8i = z[j8 + 1];
      double[] zj9r = z[j9];
      double[] zj9i = z[j9 + 1];
      double[] zj10r = z[j10];
      double[] zj10i = z[j10 + 1];
      for (int i1 = 0; i1 < n1; ++i1)
      {
        double t1r = zj1r[i1] + zj10r[i1];
        double t1i = zj1i[i1] + zj10i[i1];
        double t2r = zj2r[i1] + zj9r[i1];
        double t2i = zj2i[i1] + zj9i[i1];
        double t3r = zj3r[i1] + zj8r[i1];
        double t3i = zj3i[i1] + zj8i[i1];
        double t4r = zj4r[i1] + zj7r[i1];
        double t4i = zj4i[i1] + zj7i[i1];
        double t5r = zj5r[i1] + zj6r[i1];
        double t5i = zj5i[i1] + zj6i[i1];
        double t6r = zj1r[i1] - zj10r[i1];
        double t6i = zj1i[i1] - zj10i[i1];
        double t7r = zj2r[i1] - zj9r[i1];
        double t7i = zj2i[i1] - zj9i[i1];
        double t8r = zj3r[i1] - zj8r[i1];
        double t8i = zj3i[i1] - zj8i[i1];
        double t9r = zj4r[i1] - zj7r[i1];
        double t9i = zj4i[i1] - zj7i[i1];
        double t10r = zj5r[i1] - zj6r[i1];
        double t10i = zj5i[i1] - zj6i[i1];
        double t11r = zj0r[i1] - 0.5f * t5r;
        double t11i = zj0i[i1] - 0.5f * t5i;
        double t12r = t1r - t5r;
        double t12i = t1i - t5i;
        double t13r = t2r - t5r;
        double t13i = t2i - t5i;
        double t14r = t3r - t5r;
        double t14i = t3i - t5i;
        double t15r = t4r - t5r;
        double t15i = t4i - t5i;
        double y1r = t11r + c1 * t12r + c2 * t13r + c3 * t14r + c4 * t15r;
        double y1i = t11i + c1 * t12i + c2 * t13i + c3 * t14i + c4 * t15i;
        double y2r = t11r + c2 * t12r + c4 * t13r + c5 * t14r + c3 * t15r;
        double y2i = t11i + c2 * t12i + c4 * t13i + c5 * t14i + c3 * t15i;
        double y3r = t11r + c3 * t12r + c5 * t13r + c2 * t14r + c1 * t15r;
        double y3i = t11i + c3 * t12i + c5 * t13i + c2 * t14i + c1 * t15i;
        double y4r = t11r + c4 * t12r + c3 * t13r + c1 * t14r + c5 * t15r;
        double y4i = t11i + c4 * t12i + c3 * t13i + c1 * t14i + c5 * t15i;
        double y5r = t11r + c5 * t12r + c1 * t13r + c4 * t14r + c2 * t15r;
        double y5i = t11i + c5 * t12i + c1 * t13i + c4 * t14i + c2 * t15i;
        double y6r = c10 * t6r - c6 * t7r + c9 * t8r - c7 * t9r + c8 * t10r;
        double y6i = c10 * t6i - c6 * t7i + c9 * t8i - c7 * t9i + c8 * t10i;
        double y7r = c9 * t6r - c8 * t7r + c6 * t8r + c10 * t9r - c7 * t10r;
        double y7i = c9 * t6i - c8 * t7i + c6 * t8i + c10 * t9i - c7 * t10i;
        double y8r = c8 * t6r - c10 * t7r - c7 * t8r + c6 * t9r + c9 * t10r;
        double y8i = c8 * t6i - c10 * t7i - c7 * t8i + c6 * t9i + c9 * t10i;
        double y9r = c7 * t6r + c9 * t7r - c10 * t8r - c8 * t9r - c6 * t10r;
        double y9i = c7 * t6i + c9 * t7i - c10 * t8i - c8 * t9i - c6 * t10i;
        double y10r = c6 * t6r + c7 * t7r + c8 * t8r + c9 * t9r + c10 * t10r;
        double y10i = c6 * t6i + c7 * t7i + c8 * t8i + c9 * t9i + c10 * t10i;
        zj0r[i1] = zj0r[i1] + t1r + t2r + t3r + t4r + t5r;
        zj0i[i1] = zj0i[i1] + t1i + t2i + t3i + t4i + t5i;
        zj1r[i1] = y1r - y10i;
        zj1i[i1] = y1i + y10r;
        zj2r[i1] = y2r - y9i;
        zj2i[i1] = y2i + y9r;
        zj3r[i1] = y3r - y8i;
        zj3i[i1] = y3i + y8r;
        zj4r[i1] = y4r - y7i;
        zj4i[i1] = y4i + y7r;
        zj5r[i1] = y5r - y6i;
        zj5i[i1] = y5i + y6r;
        zj6r[i1] = y5r + y6i;
        zj6i[i1] = y5i - y6r;
        zj7r[i1] = y4r + y7i;
        zj7i[i1] = y4i - y7r;
        zj8r[i1] = y3r + y8i;
        zj8i[i1] = y3i - y8r;
        zj9r[i1] = y2r + y9i;
        zj9i[i1] = y2i - y9r;
        zj10r[i1] = y1r + y10i;
        zj10i[i1] = y1i - y10r;
      }
      int jt = j10 + 2;
      j10 = j9 + 2;
      j9 = j8 + 2;
      j8 = j7 + 2;
      j7 = j6 + 2;
      j6 = j5 + 2;
      j5 = j4 + 2;
      j4 = j3 + 2;
      j3 = j2 + 2;
      j2 = j1 + 2;
      j1 = j0 + 2;
      j0 = jt;
    }
  }

  private static void pfa13b(int n1, double[][] z, int mu, int m,
      int j0, int j1, int j2, int j3, int j4, int j5, int j6,
      int j7, int j8, int j9, int j10, int j11, int j12)
  {
    double c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12;
    if (mu == 1)
    {
      c1 = P885;
      c2 = P568;
      c3 = P120;
      c4 = -P354;
      c5 = -P748;
      c6 = -P970;
      c7 = P464;
      c8 = P822;
      c9 = P992;
      c10 = P935;
      c11 = P663;
      c12 = P239;
    }
    else if (mu == 2)
    {
      c1 = P568;
      c2 = -P354;
      c3 = -P970;
      c4 = -P748;
      c5 = P120;
      c6 = P885;
      c7 = P822;
      c8 = P935;
      c9 = P239;
      c10 = -P663;
      c11 = -P992;
      c12 = -P464;
    }
    else if (mu == 3)
    {
      c1 = P120;
      c2 = -P970;
      c3 = -P354;
      c4 = P885;
      c5 = P568;
      c6 = -P748;
      c7 = P992;
      c8 = P239;
      c9 = -P935;
      c10 = -P464;
      c11 = P822;
      c12 = P663;
    }
    else if (mu == 4)
    {
      c1 = -P354;
      c2 = -P748;
      c3 = P885;
      c4 = P120;
      c5 = -P970;
      c6 = P568;
      c7 = P935;
      c8 = -P663;
      c9 = -P464;
      c10 = P992;
      c11 = -P239;
      c12 = -P822;
    }
    else if (mu == 5)
    {
      c1 = -P748;
      c2 = P120;
      c3 = P568;
      c4 = -P970;
      c5 = P885;
      c6 = -P354;
      c7 = P663;
      c8 = -P992;
      c9 = P822;
      c10 = -P239;
      c11 = -P464;
      c12 = P935;
    }
    else if (mu == 6)
    {
      c1 = -P970;
      c2 = P885;
      c3 = -P748;
      c4 = P568;
      c5 = -P354;
      c6 = P120;
      c7 = P239;
      c8 = -P464;
      c9 = P663;
      c10 = -P822;
      c11 = P935;
      c12 = -P992;
    }
    else if (mu == 7)
    {
      c1 = -P970;
      c2 = P885;
      c3 = -P748;
      c4 = P568;
      c5 = -P354;
      c6 = P120;
      c7 = -P239;
      c8 = P464;
      c9 = -P663;
      c10 = P822;
      c11 = -P935;
      c12 = P992;
    }
    else if (mu == 8)
    {
      c1 = -P748;
      c2 = P120;
      c3 = P568;
      c4 = -P970;
      c5 = P885;
      c6 = -P354;
      c7 = -P663;
      c8 = P992;
      c9 = -P822;
      c10 = P239;
      c11 = P464;
      c12 = -P935;
    }
    else if (mu == 9)
    {
      c1 = -P354;
      c2 = -P748;
      c3 = P885;
      c4 = P120;
      c5 = -P970;
      c6 = P568;
      c7 = -P935;
      c8 = P663;
      c9 = P464;
      c10 = -P992;
      c11 = P239;
      c12 = P822;
    }
    else if (mu == 10)
    {
      c1 = P120;
      c2 = -P970;
      c3 = -P354;
      c4 = P885;
      c5 = P568;
      c6 = -P748;
      c7 = -P992;
      c8 = -P239;
      c9 = P935;
      c10 = P464;
      c11 = -P822;
      c12 = -P663;
    }
    else if (mu == 11)
    {
      c1 = P568;
      c2 = -P354;
      c3 = -P970;
      c4 = -P748;
      c5 = P120;
      c6 = P885;
      c7 = -P822;
      c8 = -P935;
      c9 = -P239;
      c10 = P663;
      c11 = P992;
      c12 = P464;
    }
    else
    {
      c1 = P885;
      c2 = P568;
      c3 = P120;
      c4 = -P354;
      c5 = -P748;
      c6 = -P970;
      c7 = -P464;
      c8 = -P822;
      c9 = -P992;
      c10 = -P935;
      c11 = -P663;
      c12 = -P239;
    }
    for (int i = 0; i < m; ++i)
    {
      double[] zj0r = z[j0];
      double[] zj0i = z[j0 + 1];
      double[] zj1r = z[j1];
      double[] zj1i = z[j1 + 1];
      double[] zj2r = z[j2];
      double[] zj2i = z[j2 + 1];
      double[] zj3r = z[j3];
      double[] zj3i = z[j3 + 1];
      double[] zj4r = z[j4];
      double[] zj4i = z[j4 + 1];
      double[] zj5r = z[j5];
      double[] zj5i = z[j5 + 1];
      double[] zj6r = z[j6];
      double[] zj6i = z[j6 + 1];
      double[] zj7r = z[j7];
      double[] zj7i = z[j7 + 1];
      double[] zj8r = z[j8];
      double[] zj8i = z[j8 + 1];
      double[] zj9r = z[j9];
      double[] zj9i = z[j9 + 1];
      double[] zj10r = z[j10];
      double[] zj10i = z[j10 + 1];
      double[] zj11r = z[j11];
      double[] zj11i = z[j11 + 1];
      double[] zj12r = z[j12];
      double[] zj12i = z[j12 + 1];
      for (int i1 = 0; i1 < n1; ++i1)
      {
        double t1r = zj1r[i1] + zj12r[i1];
        double t1i = zj1i[i1] + zj12i[i1];
        double t2r = zj2r[i1] + zj11r[i1];
        double t2i = zj2i[i1] + zj11i[i1];
        double t3r = zj3r[i1] + zj10r[i1];
        double t3i = zj3i[i1] + zj10i[i1];
        double t4r = zj4r[i1] + zj9r[i1];
        double t4i = zj4i[i1] + zj9i[i1];
        double t5r = zj5r[i1] + zj8r[i1];
        double t5i = zj5i[i1] + zj8i[i1];
        double t6r = zj6r[i1] + zj7r[i1];
        double t6i = zj6i[i1] + zj7i[i1];
        double t7r = zj1r[i1] - zj12r[i1];
        double t7i = zj1i[i1] - zj12i[i1];
        double t8r = zj2r[i1] - zj11r[i1];
        double t8i = zj2i[i1] - zj11i[i1];
        double t9r = zj3r[i1] - zj10r[i1];
        double t9i = zj3i[i1] - zj10i[i1];
        double t10r = zj4r[i1] - zj9r[i1];
        double t10i = zj4i[i1] - zj9i[i1];
        double t11r = zj5r[i1] - zj8r[i1];
        double t11i = zj5i[i1] - zj8i[i1];
        double t12r = zj6r[i1] - zj7r[i1];
        double t12i = zj6i[i1] - zj7i[i1];
        double t13r = zj0r[i1] - 0.5f * t6r;
        double t13i = zj0i[i1] - 0.5f * t6i;
        double t14r = t1r - t6r;
        double t14i = t1i - t6i;
        double t15r = t2r - t6r;
        double t15i = t2i - t6i;
        double t16r = t3r - t6r;
        double t16i = t3i - t6i;
        double t17r = t4r - t6r;
        double t17i = t4i - t6i;
        double t18r = t5r - t6r;
        double t18i = t5i - t6i;
        double y1r = t13r + c1 * t14r + c2 * t15r + c3 * t16r + c4 * t17r + c5 * t18r;
        double y1i = t13i + c1 * t14i + c2 * t15i + c3 * t16i + c4 * t17i + c5 * t18i;
        double y2r = t13r + c2 * t14r + c4 * t15r + c6 * t16r + c5 * t17r + c3 * t18r;
        double y2i = t13i + c2 * t14i + c4 * t15i + c6 * t16i + c5 * t17i + c3 * t18i;
        double y3r = t13r + c3 * t14r + c6 * t15r + c4 * t16r + c1 * t17r + c2 * t18r;
        double y3i = t13i + c3 * t14i + c6 * t15i + c4 * t16i + c1 * t17i + c2 * t18i;
        double y4r = t13r + c4 * t14r + c5 * t15r + c1 * t16r + c3 * t17r + c6 * t18r;
        double y4i = t13i + c4 * t14i + c5 * t15i + c1 * t16i + c3 * t17i + c6 * t18i;
        double y5r = t13r + c5 * t14r + c3 * t15r + c2 * t16r + c6 * t17r + c1 * t18r;
        double y5i = t13i + c5 * t14i + c3 * t15i + c2 * t16i + c6 * t17i + c1 * t18i;
        double y6r = t13r + c6 * t14r + c1 * t15r + c5 * t16r + c2 * t17r + c4 * t18r;
        double y6i = t13i + c6 * t14i + c1 * t15i + c5 * t16i + c2 * t17i + c4 * t18i;
        double y7r = c12 * t7r - c7 * t8r + c11 * t9r - c8 * t10r + c10 * t11r - c9 * t12r;
        double y7i = c12 * t7i - c7 * t8i + c11 * t9i - c8 * t10i + c10 * t11i - c9 * t12i;
        double y8r = c11 * t7r - c9 * t8r + c8 * t9r - c12 * t10r - c7 * t11r + c10 * t12r;
        double y8i = c11 * t7i - c9 * t8i + c8 * t9i - c12 * t10i - c7 * t11i + c10 * t12i;
        double y9r = c10 * t7r - c11 * t8r - c7 * t9r + c9 * t10r - c12 * t11r - c8 * t12r;
        double y9i = c10 * t7i - c11 * t8i - c7 * t9i + c9 * t10i - c12 * t11i - c8 * t12i;
        double y10r = c9 * t7r + c12 * t8r - c10 * t9r - c7 * t10r + c8 * t11r + c11 * t12r;
        double y10i = c9 * t7i + c12 * t8i - c10 * t9i - c7 * t10i + c8 * t11i + c11 * t12i;
        double y11r = c8 * t7r + c10 * t8r + c12 * t9r - c11 * t10r - c9 * t11r - c7 * t12r;
        double y11i = c8 * t7i + c10 * t8i + c12 * t9i - c11 * t10i - c9 * t11i - c7 * t12i;
        double y12r = c7 * t7r + c8 * t8r + c9 * t9r + c10 * t10r + c11 * t11r + c12 * t12r;
        double y12i = c7 * t7i + c8 * t8i + c9 * t9i + c10 * t10i + c11 * t11i + c12 * t12i;
        zj0r[i1] = zj0r[i1] + t1r + t2r + t3r + t4r + t5r + t6r;
        zj0i[i1] = zj0i[i1] + t1i + t2i + t3i + t4i + t5i + t6i;
        zj1r[i1] = y1r - y12i;
        zj1i[i1] = y1i + y12r;
        zj2r[i1] = y2r - y11i;
        zj2i[i1] = y2i + y11r;
        zj3r[i1] = y3r - y10i;
        zj3i[i1] = y3i + y10r;
        zj4r[i1] = y4r - y9i;
        zj4i[i1] = y4i + y9r;
        zj5r[i1] = y5r - y8i;
        zj5i[i1] = y5i + y8r;
        zj6r[i1] = y6r - y7i;
        zj6i[i1] = y6i + y7r;
        zj7r[i1] = y6r + y7i;
        zj7i[i1] = y6i - y7r;
        zj8r[i1] = y5r + y8i;
        zj8i[i1] = y5i - y8r;
        zj9r[i1] = y4r + y9i;
        zj9i[i1] = y4i - y9r;
        zj10r[i1] = y3r + y10i;
        zj10i[i1] = y3i - y10r;
        zj11r[i1] = y2r + y11i;
        zj11i[i1] = y2i - y11r;
        zj12r[i1] = y1r + y12i;
        zj12i[i1] = y1i - y12r;
      }
      int jt = j12 + 2;
      j12 = j11 + 2;
      j11 = j10 + 2;
      j10 = j9 + 2;
      j9 = j8 + 2;
      j8 = j7 + 2;
      j7 = j6 + 2;
      j6 = j5 + 2;
      j5 = j4 + 2;
      j4 = j3 + 2;
      j3 = j2 + 2;
      j2 = j1 + 2;
      j1 = j0 + 2;
      j0 = jt;
    }
  }

  private static void pfa16b(int n1, double[][] z, int mu, int m,
      int j0, int j1, int j2, int j3, int j4, int j5, int j6, int j7, int j8,
      int j9, int j10, int j11, int j12, int j13, int j14, int j15)
  {
    double c1, c2, c3, c4, c5, c6, c7;
    if (mu == 1)
    {
      c1 = PONE;
      c2 = P923;
      c3 = P382;
      c4 = P707;
    }
    else if (mu == 3)
    {
      c1 = -PONE;
      c2 = P382;
      c3 = P923;
      c4 = -P707;
    }
    else if (mu == 5)
    {
      c1 = PONE;
      c2 = -P382;
      c3 = P923;
      c4 = -P707;
    }
    else if (mu == 7)
    {
      c1 = -PONE;
      c2 = -P923;
      c3 = P382;
      c4 = P707;
    }
    else if (mu == 9)
    {
      c1 = PONE;
      c2 = -P923;
      c3 = -P382;
      c4 = P707;
    }
    else if (mu == 11)
    {
      c1 = -PONE;
      c2 = -P382;
      c3 = -P923;
      c4 = -P707;
    }
    else if (mu == 13)
    {
      c1 = PONE;
      c2 = P382;
      c3 = -P923;
      c4 = -P707;
    }
    else
    {
      c1 = -PONE;
      c2 = P923;
      c3 = -P382;
      c4 = P707;
    }
    c5 = c1 * c4;
    c6 = c1 * c3;
    c7 = c1 * c2;
    for (int i = 0; i < m; ++i)
    {
      double[] zj0r = z[j0];
      double[] zj0i = z[j0 + 1];
      double[] zj1r = z[j1];
      double[] zj1i = z[j1 + 1];
      double[] zj2r = z[j2];
      double[] zj2i = z[j2 + 1];
      double[] zj3r = z[j3];
      double[] zj3i = z[j3 + 1];
      double[] zj4r = z[j4];
      double[] zj4i = z[j4 + 1];
      double[] zj5r = z[j5];
      double[] zj5i = z[j5 + 1];
      double[] zj6r = z[j6];
      double[] zj6i = z[j6 + 1];
      double[] zj7r = z[j7];
      double[] zj7i = z[j7 + 1];
      double[] zj8r = z[j8];
      double[] zj8i = z[j8 + 1];
      double[] zj9r = z[j9];
      double[] zj9i = z[j9 + 1];
      double[] zj10r = z[j10];
      double[] zj10i = z[j10 + 1];
      double[] zj11r = z[j11];
      double[] zj11i = z[j11 + 1];
      double[] zj12r = z[j12];
      double[] zj12i = z[j12 + 1];
      double[] zj13r = z[j13];
      double[] zj13i = z[j13 + 1];
      double[] zj14r = z[j14];
      double[] zj14i = z[j14 + 1];
      double[] zj15r = z[j15];
      double[] zj15i = z[j15 + 1];
      for (int i1 = 0; i1 < n1; ++i1)
      {
        double t1r = zj0r[i1] + zj8r[i1];
        double t1i = zj0i[i1] + zj8i[i1];
        double t2r = zj4r[i1] + zj12r[i1];
        double t2i = zj4i[i1] + zj12i[i1];
        double t3r = zj0r[i1] - zj8r[i1];
        double t3i = zj0i[i1] - zj8i[i1];
        double t4r = c1 * (zj4r[i1] - zj12r[i1]);
        double t4i = c1 * (zj4i[i1] - zj12i[i1]);
        double t5r = t1r + t2r;
        double t5i = t1i + t2i;
        double t6r = t1r - t2r;
        double t6i = t1i - t2i;
        double t7r = zj1r[i1] + zj9r[i1];
        double t7i = zj1i[i1] + zj9i[i1];
        double t8r = zj5r[i1] + zj13r[i1];
        double t8i = zj5i[i1] + zj13i[i1];
        double t9r = zj1r[i1] - zj9r[i1];
        double t9i = zj1i[i1] - zj9i[i1];
        double t10r = zj5r[i1] - zj13r[i1];
        double t10i = zj5i[i1] - zj13i[i1];
        double t11r = t7r + t8r;
        double t11i = t7i + t8i;
        double t12r = t7r - t8r;
        double t12i = t7i - t8i;
        double t13r = zj2r[i1] + zj10r[i1];
        double t13i = zj2i[i1] + zj10i[i1];
        double t14r = zj6r[i1] + zj14r[i1];
        double t14i = zj6i[i1] + zj14i[i1];
        double t15r = zj2r[i1] - zj10r[i1];
        double t15i = zj2i[i1] - zj10i[i1];
        double t16r = zj6r[i1] - zj14r[i1];
        double t16i = zj6i[i1] - zj14i[i1];
        double t17r = t13r + t14r;
        double t17i = t13i + t14i;
        double t18r = c4 * (t15r - t16r);
        double t18i = c4 * (t15i - t16i);
        double t19r = c5 * (t15r + t16r);
        double t19i = c5 * (t15i + t16i);
        double t20r = c1 * (t13r - t14r);
        double t20i = c1 * (t13i - t14i);
        double t21r = zj3r[i1] + zj11r[i1];
        double t21i = zj3i[i1] + zj11i[i1];
        double t22r = zj7r[i1] + zj15r[i1];
        double t22i = zj7i[i1] + zj15i[i1];
        double t23r = zj3r[i1] - zj11r[i1];
        double t23i = zj3i[i1] - zj11i[i1];
        double t24r = zj7r[i1] - zj15r[i1];
        double t24i = zj7i[i1] - zj15i[i1];
        double t25r = t21r + t22r;
        double t25i = t21i + t22i;
        double t26r = t21r - t22r;
        double t26i = t21i - t22i;
        double t27r = t9r + t24r;
        double t27i = t9i + t24i;
        double t28r = t10r + t23r;
        double t28i = t10i + t23i;
        double t29r = t9r - t24r;
        double t29i = t9i - t24i;
        double t30r = t10r - t23r;
        double t30i = t10i - t23i;
        double t31r = t5r + t17r;
        double t31i = t5i + t17i;
        double t32r = t11r + t25r;
        double t32i = t11i + t25i;
        double t33r = t3r + t18r;
        double t33i = t3i + t18i;
        double t34r = c2 * t29r - c6 * t30r;
        double t34i = c2 * t29i - c6 * t30i;
        double t35r = t3r - t18r;
        double t35i = t3i - t18i;
        double t36r = c7 * t27r - c3 * t28r;
        double t36i = c7 * t27i - c3 * t28i;
        double t37r = t4r + t19r;
        double t37i = t4i + t19i;
        double t38r = c3 * t27r + c7 * t28r;
        double t38i = c3 * t27i + c7 * t28i;
        double t39r = t4r - t19r;
        double t39i = t4i - t19i;
        double t40r = c6 * t29r + c2 * t30r;
        double t40i = c6 * t29i + c2 * t30i;
        double t41r = c4 * (t12r - t26r);
        double t41i = c4 * (t12i - t26i);
        double t42r = c5 * (t12r + t26r);
        double t42i = c5 * (t12i + t26i);
        double y1r = t33r + t34r;
        double y1i = t33i + t34i;
        double y2r = t6r + t41r;
        double y2i = t6i + t41i;
        double y3r = t35r + t40r;
        double y3i = t35i + t40i;
        double y4r = t5r - t17r;
        double y4i = t5i - t17i;
        double y5r = t35r - t40r;
        double y5i = t35i - t40i;
        double y6r = t6r - t41r;
        double y6i = t6i - t41i;
        double y7r = t33r - t34r;
        double y7i = t33i - t34i;
        double y9r = t38r - t37r;
        double y9i = t38i - t37i;
        double y10r = t42r - t20r;
        double y10i = t42i - t20i;
        double y11r = t36r + t39r;
        double y11i = t36i + t39i;
        double y12r = c1 * (t11r - t25r);
        double y12i = c1 * (t11i - t25i);
        double y13r = t36r - t39r;
        double y13i = t36i - t39i;
        double y14r = t42r + t20r;
        double y14i = t42i + t20i;
        double y15r = t38r + t37r;
        double y15i = t38i + t37i;
        zj0r[i1] = t31r + t32r;
        zj0i[i1] = t31i + t32i;
        zj1r[i1] = y1r - y15i;
        zj1i[i1] = y1i + y15r;
        zj2r[i1] = y2r - y14i;
        zj2i[i1] = y2i + y14r;
        zj3r[i1] = y3r - y13i;
        zj3i[i1] = y3i + y13r;
        zj4r[i1] = y4r - y12i;
        zj4i[i1] = y4i + y12r;
        zj5r[i1] = y5r - y11i;
        zj5i[i1] = y5i + y11r;
        zj6r[i1] = y6r - y10i;
        zj6i[i1] = y6i + y10r;
        zj7r[i1] = y7r - y9i;
        zj7i[i1] = y7i + y9r;
        zj8r[i1] = t31r - t32r;
        zj8i[i1] = t31i - t32i;
        zj9r[i1] = y7r + y9i;
        zj9i[i1] = y7i - y9r;
        zj10r[i1] = y6r + y10i;
        zj10i[i1] = y6i - y10r;
        zj11r[i1] = y5r + y11i;
        zj11i[i1] = y5i - y11r;
        zj12r[i1] = y4r + y12i;
        zj12i[i1] = y4i - y12r;
        zj13r[i1] = y3r + y13i;
        zj13i[i1] = y3i - y13r;
        zj14r[i1] = y2r + y14i;
        zj14i[i1] = y2i - y14r;
        zj15r[i1] = y1r + y15i;
        zj15i[i1] = y1i - y15r;
      }
      int jt = j15 + 2;
      j15 = j14 + 2;
      j14 = j13 + 2;
      j13 = j12 + 2;
      j12 = j11 + 2;
      j11 = j10 + 2;
      j10 = j9 + 2;
      j9 = j8 + 2;
      j8 = j7 + 2;
      j7 = j6 + 2;
      j6 = j5 + 2;
      j5 = j4 + 2;
      j4 = j3 + 2;
      j3 = j2 + 2;
      j2 = j1 + 2;
      j1 = j0 + 2;
      j0 = jt;
    }
  }

  // Constants used in this implementation of the prime-factor FFT.
  private static final double P120      = 0.120536680;
  private static final double P142      = 0.142314838;
  private static final double P173      = 0.173648178;
  private static final double P222      = 0.222520934;
  private static final double P239      = 0.239315664;
  private static final double P281      = 0.281732557;
  private static final double P342      = 0.342020143;
  private static final double P354      = 0.354604887;
  private static final double P382      = 0.382683432;
  private static final double P415      = 0.415415013;
  private static final double P433      = 0.433883739;
  private static final double P464      = 0.464723172;
  private static final double P540      = 0.540640817;
  private static final double P559      = 0.559016994;
  private static final double P568      = 0.568064747;
  private static final double P587      = 0.587785252;
  private static final double P623      = 0.623489802;
  private static final double P642      = 0.642787610;
  private static final double P654      = 0.654860734;
  private static final double P663      = 0.663122658;
  private static final double P707      = 0.707106781;
  private static final double P748      = 0.748510748;
  private static final double P755      = 0.755749574;
  private static final double P766      = 0.766044443;
  private static final double P781      = 0.781831482;
  private static final double P822      = 0.822983866;
  private static final double P841      = 0.841253533;
  private static final double P866      = 0.866025404;
  private static final double P885      = 0.885456026;
  private static final double P900      = 0.900968868;
  private static final double P909      = 0.909631995;
  private static final double P923      = 0.923879533;
  private static final double P935      = 0.935016243;
  private static final double P939      = 0.939692621;
  private static final double P951      = 0.951056516;
  private static final double P959      = 0.959492974;
  private static final double P970      = 0.970941817;
  private static final double P974      = 0.974927912;
  private static final double P984      = 0.984807753;
  private static final double P989      = 0.989821442;
  private static final double P992      = 0.992708874;
  private static final double PONE      = 1.000000000;

  // Factors supported in this implementation of the prime-factor FFT.
  // Methods above require that these factors be in descending order.
  private static final int    NFAC      = 10;
  private static final int    _kfac[]   = {
      16, 13, 11, 9, 8, 7, 5, 4, 3, 2
  };

  // FFT lengths supported in this implementation of the prime-factor FFT.
  // These lengths are the products of mutually prime factors chosen from
  // the set above. For example, note that 17 and 32 are not in this table;
  // 17 is not in the set above, and 32 = 2*16 is not valid, because the
  // factors 2 and 16 share the prime factor 2.
  private static final int    NTABLE    = 240;
  private static final int    _ntable[] = {
      1, 2, 3, 4, 5, 6, 7, 8, 9,
      10, 11, 12, 13, 14, 15, 16, 18, 20, 21, 22, 24, 26, 28, 30, 33,
      35, 36, 39, 40, 42, 44, 45, 48, 52, 55, 56, 60, 63, 65, 66, 70,
      72, 77, 78, 80, 84, 88, 90, 91, 99,
      104, 105, 110, 112, 117, 120, 126, 130, 132, 140, 143, 144, 154,
      156, 165, 168, 176, 180, 182, 195, 198, 208, 210, 220, 231, 234,
      240, 252, 260, 264, 273, 280, 286, 308, 312, 315, 330, 336, 360,
      364, 385, 390, 396, 420, 429, 440, 455, 462, 468, 495, 504, 520,
      528, 546, 560, 572, 585, 616, 624, 630, 660, 693, 715, 720, 728,
      770, 780, 792, 819, 840, 858, 880, 910, 924, 936, 990,
      1001, 1008, 1040, 1092, 1144, 1155, 1170, 1232, 1260, 1287, 1320,
      1365, 1386, 1430, 1456, 1540, 1560, 1584, 1638, 1680, 1716, 1820,
      1848, 1872, 1980, 2002, 2145, 2184, 2288, 2310, 2340, 2520, 2574,
      2640, 2730, 2772, 2860, 3003, 3080, 3120, 3276, 3432, 3465, 3640,
      3696, 3960, 4004, 4095, 4290, 4368, 4620, 4680, 5005, 5040, 5148,
      5460, 5544, 5720, 6006, 6160, 6435, 6552, 6864, 6930, 7280, 7920,
      8008, 8190, 8580, 9009, 9240, 9360,
      10010, 10296, 10920, 11088, 11440, 12012, 12870, 13104, 13860,
      15015, 16016, 16380, 17160, 18018, 18480, 20020, 20592, 21840,
      24024, 25740, 27720, 30030, 32760, 34320, 36036, 40040, 45045,
      48048, 51480, 55440, 60060, 65520, 72072, 80080, 90090,
      102960, 120120, 144144, 180180, 240240, 360360, 720720
  };

  // FFT costs, one for each FFT length above. These are times, in seconds,
  // for a forward and inverse FFT and scaling (by 1.0/nfft), as measured
  // by Dave Hale, 03/25/2005, on a Pentium M 2.1 GHz processor, using a
  // Java Hotspot Server VM (build 1.5.0_01-b08, mixed mode).
  private static final double _ctable[] = {
      0.00000154844595, 0.00000160858985, 0.00000173777398, 0.00000178300246,
      0.00000186692603, 0.00000202796424, 0.00000205593203, 0.00000203027471,
      0.00000213199871, 0.00000223464061, 0.00000245504197, 0.00000224507775,
      0.00000277484785, 0.00000271335681, 0.00000260084271, 0.00000266712117,
      0.00000277849063, 0.00000284002694, 0.00000317837121, 0.00000373373597,
      0.00000315791133, 0.00000424124687, 0.00000358681599, 0.00000374904075,
      0.00000474708669, 0.00000438838644, 0.00000401250829, 0.00000562735292,
      0.00000434116390, 0.00000517084182, 0.00000552020262, 0.00000498530294,
      0.00000520248930, 0.00000681986102, 0.00000710553295, 0.00000593798174,
      0.00000587481339, 0.00000701743322, 0.00000861901953, 0.00000832813604,
      0.00000791730898, 0.00000688403680, 0.00001002803645, 0.00001026784570,
      0.00000819893573, 0.00000828260941, 0.00001014724144, 0.00000934954606,
      0.00001232645727, 0.00001214189591, 0.00001269497208, 0.00001102202755,
      0.00001388176589, 0.00001172641106, 0.00001503375461, 0.00001113972204,
      0.00001364655225, 0.00001703912278, 0.00001477596306, 0.00001424973677,
      0.00002127456187, 0.00001398938399, 0.00002008644290, 0.00001869909587,
      0.00001986433148, 0.00001651781665, 0.00002092824006, 0.00001702478496,
      0.00002499906394, 0.00002500343282, 0.00002465807389, 0.00002632305568,
      0.00002308853872, 0.00002566435179, 0.00002996843066, 0.00003179869821,
      0.00002372351388, 0.00002578195392, 0.00003236648622, 0.00003062192175,
      0.00003688562326, 0.00002903319322, 0.00004464405117, 0.00003835181037,
      0.00003890755813, 0.00003489790229, 0.00004193095941, 0.00003661590772,
      0.00003585050946, 0.00004948000296, 0.00005194636790, 0.00005316664012,
      0.00004831628715, 0.00004470982143, 0.00006711791710, 0.00005416880764,
      0.00006503589644, 0.00006376341005, 0.00006060697752, 0.00006481361636,
      0.00005494746660, 0.00006842950360, 0.00006725764749, 0.00007955041900,
      0.00006517351390, 0.00008783546746, 0.00008145918907, 0.00008208007212,
      0.00008453260181, 0.00007714824943, 0.00008332986646, 0.00009686623465,
      0.00011742291007, 0.00008016979016, 0.00010372863801, 0.00011169975463,
      0.00010527145635, 0.00010259693695, 0.00012171112596, 0.00009645574497,
      0.00014179527113, 0.00011871442125, 0.00014121545403, 0.00012558781115,
      0.00012962723272, 0.00014012872534, 0.00017282139776, 0.00012333743842,
      0.00015017243965, 0.00015933147632, 0.00018467637839, 0.00016783978549,
      0.00017760241178, 0.00018111945022, 0.00015790303508, 0.00021759913091,
      0.00017785473273, 0.00021290391156, 0.00021022786937, 0.00025198138131,
      0.00022553766468, 0.00022349921892, 0.00022576645627, 0.00022422478451,
      0.00026572035023, 0.00021417878529, 0.00028409252164, 0.00028290960452,
      0.00026774495388, 0.00028504340401, 0.00028006152125, 0.00037010347376,
      0.00037949981053, 0.00033613022319, 0.00040431974162, 0.00036459661264,
      0.00035351217790, 0.00033107438017, 0.00046689976690, 0.00039452432539,
      0.00045647219690, 0.00042150673401, 0.00050466112371, 0.00055797101449,
      0.00047941598851, 0.00049189587426, 0.00052933403805, 0.00060568491080,
      0.00056200897868, 0.00060222489477, 0.00058842538190, 0.00060084033613,
      0.00074850523169, 0.00070305370305, 0.00081422764228, 0.00073423753666,
      0.00073504587156, 0.00075785092698, 0.00098138167565, 0.00073504587156,
      0.00093946503989, 0.00091880733945, 0.00090674513354, 0.00105810882198,
      0.00120590006020, 0.00104322916667, 0.00124255583127, 0.00113291855204,
      0.00130338541667, 0.00122432762836, 0.00130234070221, 0.00133444370420,
      0.00158214849921, 0.00153958493467, 0.00166694421316, 0.00183424908425,
      0.00157344854674, 0.00164180327869, 0.00210620399579, 0.00198316831683,
      0.00195414634146, 0.00197145669291, 0.00228132118451, 0.00241495778046,
      0.00264248021108, 0.00243970767357, 0.00246068796069, 0.00314937106918,
      0.00341226575809, 0.00304407294833, 0.00342979452055, 0.00391780821918,
      0.00339491525424, 0.00423467230444, 0.00427991452991, 0.00422573839662,
      0.00513589743590, 0.00532712765957, 0.00522976501305, 0.00671812080537,
      0.00644051446945, 0.00747388059701, 0.00785490196078, 0.00890222222222,
      0.01032474226804, 0.01088586956522, 0.01131638418079, 0.01125280898876,
      0.01371232876712, 0.01390972222222, 0.01663636363636, 0.01917142857143,
      0.02201098901099, 0.02425301204819, 0.02849295774648, 0.03531578947368,
      0.04575000000000, 0.06190909090909, 0.10542105263158, 0.24033333333333,
  };

  /**
   * Performs a search in a monotonic array of values. Values are
   * assumed to increase, with no equal values.
   * <p>
   * @param a the array of values, assumed to be monotonic.
   * @param x the value for which to search.
   * @return the index at which the specified value is found, or, if not
   *  found, -(i+1), where i equals the index at which the specified value
   *  would be located if it was inserted into the monotonic array.
   */
  public static int search(int[] a, int x)
  {
    int res = -a.length;
    for (int i = 0; i < a.length; i++)
    {
      if (a[i] == x)
      {
        return i;
      }
      if (a[i] > x)
      {
        return -i;
      }
      if (a[i] < x)
      {
        res = -(i + 1);
      }
    }
    return res;
  }

}