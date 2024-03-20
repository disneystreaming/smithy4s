package smithy4s.protobuf.internals

import com.google.protobuf.CodedOutputStream
import com.google.protobuf.CodedInputStream

/** A codec for primitive types that are length-delimited
  */
// scalafmt: {maxColumn = 120}
private[internals] sealed trait NonScalarPrimitiveCodec[A] extends PrimitiveCodec[A] { self =>
  final def wireType: Int = Wire.WireType.LengthDelimited
}

private[internals] object NonScalarPrimitiveCodec {

  object StringCodec extends NonScalarPrimitiveCodec[String] {
    def zero: String = ""
    def isZero(a: String) = a.isEmpty()
    def sizeNoTag(a: String): Int = CodedOutputStream.computeStringSizeNoTag(a)
    def sizeTag(tag: Int, a: String): Int = CodedOutputStream.computeStringSize(tag, a)
    def writeNoTag(a: String, os: CodedOutputStream): Unit = os.writeStringNoTag(a)
    def writeTag(tag: Int, a: String, os: CodedOutputStream): Unit = os.writeString(tag, a)
    def read(is: CodedInputStream): String = is.readString()
  }

  object ByteArrayCodec extends NonScalarPrimitiveCodec[Array[Byte]] {
    def zero: Array[Byte] = Array.empty
    def isZero(a: Array[Byte]) = a.isEmpty
    def sizeNoTag(a: Array[Byte]): Int = CodedOutputStream.computeByteArraySizeNoTag(a)
    def sizeTag(tag: Int, a: Array[Byte]): Int = CodedOutputStream.computeByteArraySize(tag, a)
    def writeNoTag(a: Array[Byte], os: CodedOutputStream): Unit = os.writeByteArrayNoTag(a)
    def writeTag(tag: Int, a: Array[Byte], os: CodedOutputStream): Unit = os.writeByteArray(tag, a)
    def read(is: CodedInputStream): Array[Byte] = is.readByteArray()
  }

}
