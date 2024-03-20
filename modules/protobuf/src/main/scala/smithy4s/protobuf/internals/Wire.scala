package smithy4s.protobuf.internals

object Wire {

  def encodeTag(protoIndex: Int, wireType: Int): Int =
    (protoIndex << 3) | wireType

  def decodeTag(byte: Byte): (Int, Int) = {
    val m = byte & 0xff
    (m >> 3, m & 0x7)
  }

  // See https://protobuf.dev/programming-guides/encoding/#structure
  //
  // NB : not ascribing types to get special encoding in bytecode.
  object WireType {
    val Varint = 0x00
    val Fixed64 = 0x01
    val LengthDelimited = 0x02
    val Fixed32 = 0x05
  }

}
