package axs7sds.midi;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import axs7sds.midi.ByteConverter;

public class ByteConverterTest
{

  @Test
  public void testByteFrom2Bytes7()
  {
    assertEquals(255, ByteConverter.byteFrom2Bytes7(new byte[]{127, 1}, 0));
  }

  @Test
  public void testByteTo2Bytes7()
  {
    byte[] b = new byte[2];
    ByteConverter.byteTo2Bytes7(255, b, 0);
    assertArrayEquals(new byte[]{127, 1}, b);
  }

  @Test
  public void testIntFrom3Bytes()
  {
    assertEquals(65535, ByteConverter.intFrom3Bytes(new byte[]{127, 127, 3}, 0));
  }

  @Test
  public void testIntTo3Bytes()
  {
    byte[] b = new byte[3];
    ByteConverter.intTo3Bytes(65535, b, 0);
    assertArrayEquals(new byte[]{127, 127, 3}, b);
  }

  @Test
  public void testInt12BitFrom2Bytes()
  {
    assertEquals(4095, ByteConverter.int12BitFrom2Bytes(new byte[]{127, 124}, 0));
  }

  @Test
  public void testInt12BitTo2Bytes()
  {
    byte[] b = new byte[2];
    ByteConverter.int12BitTo2Bytes(4095, b, 0);
    assertArrayEquals(new byte[]{127, 124}, b);
  }

  @Test
  public void testInt16BitFrom4Bytes()
  {
    assertEquals(65535, ByteConverter.int16BitFrom4Bytes(new byte[]{127, 1, 127, 1}, 0));
  }

  @Test
  public void testInt16BitTo4Bytes()
  {
    byte[] b = new byte[4];
    ByteConverter.int16BitTo4Bytes(65535, b, 0);
    assertArrayEquals(new byte[]{127, 1, 127, 1}, b);
  }

  @Test
  public void testAkaiValues()
  {
    //values return from x7000 by new sample created with default values
    
    //Sampling period in nS 
    assertEquals(63704, ByteConverter.intFrom3Bytes(new byte[]{0x58, 0x71, 0x03}, 0));
    //Total words in sample
    assertEquals(32752, ByteConverter.intFrom3Bytes(new byte[]{0x70, 0x7f, 0x01}, 0));
  }
}
