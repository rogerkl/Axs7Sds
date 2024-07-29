package axs7sds.midi;

/**
 * Convert midi bit packings used by akai sysex
 *
 * - Coding of types B, DB, TB, DW and SW:
 * - B represents a byte transmitted as a single MIDI byte.
 * - DB represents a 8bit value transmitted as 2 MIDI bytes:
 * first byte: 0 d6 d5 d4 d3 d2 d1 d0
 * second byte: 0 0 0 0 0 0 0 d7
 * - TB represents a 21bit value transmitted as 3 MIDI bytes:
 * first byte: 0 d6 d5 d4 d3 d2 d1 d0
 * second byte: 0 d13 d12 d11 d10 d9 d8 d7
 * third byte: 0 d20 d19 d18 d17 d16 d15 d14
 * - SW represents a 12bit value transmitted as 2 MIDI bytes:
 * first byte: 0 d11 d10 d9 d8 d7 d6 d5
 * second byte: 0 d4 d3 d2 d1 d0 0 0
 * - DW represents a 16bit value transmitted as 4 MIDI bytes:
 * first byte: 0 d6 d5 d4 d3 d2 d1 d0
 * second byte: 0 0 0 0 0 0 0 d7
 * third byte: 0 d14 d13 d12 d11 d10 d9 d8
 * fourth byte: 0 0 0 0 0 0 0 d15
 */
public class ByteConverter
{
  /*
   * get unsigned byte value from 2 7 bit bytes
   * - DB represents a 8bit value transmitted as 2 MIDI bytes:
   * first byte: 0 d6 d5 d4 d3 d2 d1 d0
   * second byte: 0 0 0 0 0 0 0 d7
   */
  public static int byteFrom2Bytes7(byte[] bytes, int index)
  {
    return (bytes[index] + (bytes[index + 1] << 7));
  }

  /*
   * get 2 7-bit bytes from unsigned byte
   * - DB represents a 8bit value transmitted as 2 MIDI bytes:
   * first byte: 0 d6 d5 d4 d3 d2 d1 d0
   * second byte: 0 0 0 0 0 0 0 d7
   */
  public static void byteTo2Bytes7(int value, byte[] bytes, int index)
  {
    bytes[index] = (byte) (0b0000000001111111 & value);
    bytes[index + 1] = (byte) (value >>> 7);
  }

  /*
   * get int value from 3 7 bit bytes
   * - TB represents a 21bit value transmitted as 3 MIDI bytes:
   * first byte: 0 d6 d5 d4 d3 d2 d1 d0
   * second byte: 0 d13 d12 d11 d10 d9 d8 d7
   * third byte: 0 d20 d19 d18 d17 d16 d15 d14
   */
  public static int intFrom3Bytes(byte[] bytes, int index)
  {
    return bytes[index] + (bytes[index + 1] << 7) + (bytes[index + 2] << 14);
  }

  /*
   * get 3 7 bit bytes from int value
   * - TB represents a 21bit value transmitted as 3 MIDI bytes:
   * first byte: 0 d6 d5 d4 d3 d2 d1 d0
   * second byte: 0 d13 d12 d11 d10 d9 d8 d7
   * third byte: 0 d20 d19 d18 d17 d16 d15 d14
   */
  public static void intTo3Bytes(int value, byte[] bytes, int index)
  {
    bytes[index] = (byte) (0b0000000001111111 & value);
    bytes[index + 1] = (byte) (0b0000000001111111 & (value >>> 7));
    bytes[index + 2] = (byte) (value >>> 14);
  }

  /*
   * get 12 bit int value from 2 7 bit bytes
   * - SW represents a 12bit value transmitted as 2 MIDI bytes:
   * first byte: 0 d4 d3 d2 d1 d0 0 0 
   * second byte: 0 d11 d10 d9 d8 d7 d6 d5
   */
  public static int int12BitFrom2Bytes(byte[] bytes, int index)
  {
    return (bytes[index + 1] >>> 2) + (bytes[index] << 5);
  }

  /*
   * get 2 7 bit bytes from 12 bit int value
   * - SW represents a 12bit value transmitted as 2 MIDI bytes:
   * first byte: 0 d4 d3 d2 d1 d0 0 0 
   * second byte: 0 d11 d10 d9 d8 d7 d6 d5
   */
  public static void int12BitTo2Bytes(int value, byte[] bytes, int index)
  {
    bytes[index] = (byte) (0b0000000001111111 & (value >>> 5));
    bytes[index + 1] = (byte) (0b0000000001111100 & (value << 2));
  }

  /*
   * get 16 bit int value from 4 7 bit bytes
   * - DW represents a 16bit value transmitted as 4 MIDI bytes:
   * first byte: 0 d6 d5 d4 d3 d2 d1 d0
   * second byte: 0 0 0 0 0 0 0 d7
   * third byte: 0 d14 d13 d12 d11 d10 d9 d8
   * fourth byte: 0 0 0 0 0 0 0 d15
   */
  public static int int16BitFrom4Bytes(byte[] bytes, int index)
  {
    return bytes[index] + (bytes[index + 1] << 7) + (bytes[index + 2] << 8)
        + (bytes[index + 3] << 15);
  }

  /*
   * get 4 7 bit bytes from 16 bit int value
   * - DW represents a 16bit value transmitted as 4 MIDI bytes:
   * first byte: 0 d6 d5 d4 d3 d2 d1 d0
   * second byte: 0 0 0 0 0 0 0 d7
   * third byte: 0 d14 d13 d12 d11 d10 d9 d8
   * fourth byte: 0 0 0 0 0 0 0 d15
   */
  public static void int16BitTo4Bytes(int value, byte[] bytes, int index)
  {
    bytes[index] = (byte) (0b0000000001111111 & value);
    bytes[index + 1] = (byte) (0b0000000000000001 & (value >>> 7));
    bytes[index + 2] = (byte) (0b0000000001111111 & (value >>> 8));
    bytes[index + 3] = (byte) (0b0000000000000001 & (value >>> 15));
  }

  static void printBytes(byte[] bytes, int index, int length)
  {
    System.out.print("[");
    for (int i = index; i < index + length; i++)
    {
      if (i > 0)
      {
        System.out.print(",");
      }
      System.out.print(bytes[i]);
    }
    System.out.println("]");
  }

  public static void main(String[] args)
  {
    byte[] b = new byte[4];

    byteTo2Bytes7(255, b, 0);
    printBytes(b, 0, 2);
    System.out.println(byteFrom2Bytes7(b, 0));

    intTo3Bytes(65535, b, 0);
    printBytes(b, 0, 3);
    System.out.println(intFrom3Bytes(b, 0));

    int12BitTo2Bytes(4095, b, 0);
    printBytes(b, 0, 2);
    System.out.println(int12BitFrom2Bytes(b, 0));

    int12BitTo2Bytes(0, b, 0);
    printBytes(b, 0, 2);
    System.out.println(int12BitFrom2Bytes(b, 0));

    int12BitTo2Bytes(2047, b, 0);
    printBytes(b, 0, 2);
    System.out.println(int12BitFrom2Bytes(b, 0));

    int16BitTo4Bytes(65535, b, 0);
    printBytes(b, 0, 4);
    System.out.println(int16BitFrom4Bytes(b, 0));

    System.out.println(intFrom3Bytes(new byte[]{0x58, 0x71, 0x03}, 0));
    System.out.println(intFrom3Bytes(new byte[]{0x70, 0x7f, 0x01}, 0));
  }

}
