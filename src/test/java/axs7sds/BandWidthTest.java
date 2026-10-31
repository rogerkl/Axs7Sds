package axs7sds;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import axs7sds.SampleDump.BandWidth;
import axs7sds.SampleDump.LoopMode;
import axs7sds.SampleDump.SampleHeader;

public class BandWidthTest
{
  @Test
  public void nearestTableRate()
  {
    assertEquals(BandWidth.BW16, BandWidth.nearest(48000));
    assertEquals(BandWidth.BW16, BandWidth.nearest(44100));
    assertEquals(BandWidth.BW16, BandWidth.nearest(39555));
    assertEquals(BandWidth.BW8_9, BandWidth.nearest(22050)); // 22199.53 Hz, 12 cents away
    assertEquals(BandWidth.BW4_0, BandWidth.nearest(10000)); // 9888.85 Hz, 19 cents away
    assertEquals(BandWidth.BW1_6, BandWidth.nearest(3000));
  }

  @Test
  public void wavFileRateMatchesTableRate()
  {
    assertTrue(BandWidth.BW16.matches(39555)); // a received sample saved as a wav file
    assertTrue(BandWidth.BW8_4.matches(20954));
    assertFalse(BandWidth.BW16.matches(44100));
  }

  @Test
  public void headerHasExactTablePeriod()
  {
    assertEquals(25281, new SampleHeader(0, 12, 39555.4f, 1000, 0, 900, LoopMode.Looping).period());
    assertEquals(25281, new SampleHeader(0, 12, 39555f, 1000, 0, 900, LoopMode.Looping).period());
    assertEquals(22675, new SampleHeader(0, 12, 44100f, 1000, 0, 900, LoopMode.Looping).period());
  }
}
