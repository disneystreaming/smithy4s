/*
 *  Copyright 2021-2025 Disney Streaming
 *
 *  Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     https://disneystreaming.github.io/TOST-1.0.txt
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package smithy4s.protobuf.internals

import alloy.proto.ProtoNumType
import alloy.proto.ProtoNumType._
import com.google.protobuf.CodedInputStream
import com.google.protobuf.CodedOutputStream

/** A codec for primitive types that are considered Scalar (that is with a wire
  * type that is not length-delimited)
  */
// scalafmt: {maxColumn = 120}
sealed trait ScalarCodec[A] extends PrimitiveCodec[A] { self =>
  final def isZero(a: A): Boolean = a == zero

  final def imap[B](to: A => B, from: B => A): ScalarCodec[B] = new ScalarCodec[B] {
    def zero: B = to(self.zero)
    def wireType: Int = self.wireType
    def sizeNoTag(b: B): Int = self.sizeNoTag(from(b))
    def sizeTag(field: Int, b: B): Int = self.sizeTag(field, from(b))
    def writeNoTag(b: B, os: CodedOutputStream): Unit = self.writeNoTag(from(b), os)
    def writeTag(field: Int, b: B, os: CodedOutputStream): Unit = self.writeTag(field, from(b), os)
    def read(is: CodedInputStream): B = to(self.read(is))
  }
}

private[protobuf] object ScalarCodec {

  object BooleanCodec extends ScalarCodec[Boolean] {
    def zero: Boolean = false
    def wireType: Int = Wire.WireType.Varint
    def sizeNoTag(a: Boolean): Int = CodedOutputStream.computeBoolSizeNoTag(a)
    def sizeTag(field: Int, a: Boolean): Int = CodedOutputStream.computeBoolSize(field, a)
    def writeNoTag(a: Boolean, os: CodedOutputStream): Unit = os.writeBoolNoTag(a)
    def writeTag(field: Int, a: Boolean, os: CodedOutputStream): Unit = os.writeBool(field, a)
    def read(is: CodedInputStream): Boolean = is.readBool()
  }

  def intCodec(maybeNumType: Option[ProtoNumType]): ScalarCodec[Int] = maybeNumType match {
    case None               => IntCodec
    case Some(FIXED)        => FixedIntCodec
    case Some(FIXED_SIGNED) => SignedFixedIntCodec
    case Some(SIGNED)       => SignedIntCodec
    case Some(UNSIGNED)     => UnsignedIntCodec
  }

  object IntCodec extends ScalarCodec[Int] {
    def zero: Int = 0
    def wireType: Int = Wire.WireType.Varint
    def sizeNoTag(a: Int): Int = CodedOutputStream.computeInt32SizeNoTag(a)
    def sizeTag(field: Int, a: Int): Int = CodedOutputStream.computeInt32Size(field, a)
    def writeNoTag(a: Int, os: CodedOutputStream): Unit = os.writeInt32NoTag(a)
    def writeTag(field: Int, a: Int, os: CodedOutputStream): Unit = os.writeInt32(field, a)
    def read(is: CodedInputStream): Int = is.readInt32()
  }

  object SignedIntCodec extends ScalarCodec[Int] {
    def zero: Int = 0
    def wireType: Int = Wire.WireType.Varint
    def sizeNoTag(a: Int): Int = CodedOutputStream.computeSInt32SizeNoTag(a)
    def sizeTag(field: Int, a: Int): Int = CodedOutputStream.computeSInt32Size(field, a)
    def writeNoTag(a: Int, os: CodedOutputStream): Unit = os.writeSInt32NoTag(a)
    def writeTag(field: Int, a: Int, os: CodedOutputStream): Unit = os.writeSInt32(field, a)
    def read(is: CodedInputStream): Int = is.readSInt32()
  }

  object UnsignedIntCodec extends ScalarCodec[Int] {
    def zero: Int = 0
    def wireType: Int = Wire.WireType.Varint
    def sizeNoTag(a: Int): Int = CodedOutputStream.computeUInt32SizeNoTag(a)
    def sizeTag(field: Int, a: Int): Int = CodedOutputStream.computeUInt32Size(field, a)
    def writeNoTag(a: Int, os: CodedOutputStream): Unit = os.writeUInt32NoTag(a)
    def writeTag(field: Int, a: Int, os: CodedOutputStream): Unit = os.writeUInt32(field, a)
    def read(is: CodedInputStream): Int = is.readUInt32()
  }

  object FixedIntCodec extends ScalarCodec[Int] {
    def zero: Int = 0
    def wireType: Int = Wire.WireType.Fixed32
    def sizeNoTag(a: Int): Int = CodedOutputStream.computeFixed32SizeNoTag(a)
    def sizeTag(field: Int, a: Int): Int = CodedOutputStream.computeFixed32Size(field, a)
    def writeNoTag(a: Int, os: CodedOutputStream): Unit = os.writeFixed32NoTag(a)
    def writeTag(field: Int, a: Int, os: CodedOutputStream): Unit = os.writeFixed32(field, a)
    def read(is: CodedInputStream): Int = is.readFixed32()
  }

  object SignedFixedIntCodec extends ScalarCodec[Int] {
    def zero: Int = 0
    def wireType: Int = Wire.WireType.Fixed32
    def sizeNoTag(a: Int): Int = CodedOutputStream.computeSFixed32SizeNoTag(a)
    def sizeTag(field: Int, a: Int): Int = CodedOutputStream.computeSFixed32Size(field, a)
    def writeNoTag(a: Int, os: CodedOutputStream): Unit = os.writeSFixed32NoTag(a)
    def writeTag(field: Int, a: Int, os: CodedOutputStream): Unit = os.writeSFixed32(field, a)
    def read(is: CodedInputStream): Int = is.readSFixed32()
  }

  val ByteCodec = IntCodec.imap[Byte](_.toByte, _.toInt)
  val ShortCodec = IntCodec.imap[Short](_.toShort, _.toInt)

  def longCodec(maybeNumType: Option[ProtoNumType]): ScalarCodec[Long] = maybeNumType match {
    case None               => LongCodec
    case Some(FIXED)        => FixedLongCodec
    case Some(FIXED_SIGNED) => SignedFixedLongCodec
    case Some(SIGNED)       => SignedLongCodec
    case Some(UNSIGNED)     => UnsignedLongCodec
  }

  object LongCodec extends ScalarCodec[Long] {
    def zero: Long = 0
    def wireType: Int = Wire.WireType.Varint
    def sizeNoTag(a: Long): Int = CodedOutputStream.computeInt64SizeNoTag(a)
    def sizeTag(field: Int, a: Long): Int = CodedOutputStream.computeInt64Size(field, a)
    def writeNoTag(a: Long, os: CodedOutputStream): Unit = os.writeInt64NoTag(a)
    def writeTag(field: Int, a: Long, os: CodedOutputStream): Unit = os.writeInt64(field, a)
    def read(is: CodedInputStream): Long = is.readInt64()
  }

  object SignedLongCodec extends ScalarCodec[Long] {
    def zero: Long = 0
    def wireType: Int = Wire.WireType.Varint
    def sizeNoTag(a: Long): Int = CodedOutputStream.computeSInt64SizeNoTag(a)
    def sizeTag(field: Int, a: Long): Int = CodedOutputStream.computeSInt64Size(field, a)
    def writeNoTag(a: Long, os: CodedOutputStream): Unit = os.writeSInt64NoTag(a)
    def writeTag(field: Int, a: Long, os: CodedOutputStream): Unit = os.writeSInt64(field, a)
    def read(is: CodedInputStream): Long = is.readSInt64()
  }

  object UnsignedLongCodec extends ScalarCodec[Long] {
    def zero: Long = 0
    def wireType: Int = Wire.WireType.Varint
    def sizeNoTag(a: Long): Int = CodedOutputStream.computeUInt64SizeNoTag(a)
    def sizeTag(field: Int, a: Long): Int = CodedOutputStream.computeUInt64Size(field, a)
    def writeNoTag(a: Long, os: CodedOutputStream): Unit = os.writeUInt64NoTag(a)
    def writeTag(field: Int, a: Long, os: CodedOutputStream): Unit = os.writeUInt64(field, a)
    def read(is: CodedInputStream): Long = is.readUInt64()
  }

  object FixedLongCodec extends ScalarCodec[Long] {
    def zero: Long = 0
    def wireType: Int = Wire.WireType.Fixed64
    def sizeNoTag(a: Long): Int = CodedOutputStream.computeFixed64SizeNoTag(a)
    def sizeTag(field: Int, a: Long): Int = CodedOutputStream.computeFixed64Size(field, a)
    def writeNoTag(a: Long, os: CodedOutputStream): Unit = os.writeFixed64NoTag(a)
    def writeTag(field: Int, a: Long, os: CodedOutputStream): Unit = os.writeFixed64(field, a)
    def read(is: CodedInputStream): Long = is.readFixed64()
  }

  object SignedFixedLongCodec extends ScalarCodec[Long] {
    def zero: Long = 0
    def wireType: Int = Wire.WireType.Fixed64
    def sizeNoTag(a: Long): Int = CodedOutputStream.computeSFixed64SizeNoTag(a)
    def sizeTag(field: Int, a: Long): Int = CodedOutputStream.computeSFixed64Size(field, a)
    def writeNoTag(a: Long, os: CodedOutputStream): Unit = os.writeSFixed64NoTag(a)
    def writeTag(field: Int, a: Long, os: CodedOutputStream): Unit = os.writeSFixed64(field, a)
    def read(is: CodedInputStream): Long = is.readSFixed64()
  }

  object DoubleCodec extends ScalarCodec[Double] {
    def zero: Double = 0
    def wireType: Int = Wire.WireType.Fixed64
    def sizeNoTag(a: Double): Int = CodedOutputStream.computeDoubleSizeNoTag(a)
    def sizeTag(field: Int, a: Double): Int = CodedOutputStream.computeDoubleSize(field, a)
    def writeNoTag(a: Double, os: CodedOutputStream): Unit = os.writeDoubleNoTag(a)
    def writeTag(field: Int, a: Double, os: CodedOutputStream): Unit = os.writeDouble(field, a)
    def read(is: CodedInputStream): Double = is.readDouble()
  }

  object FloatCodec extends ScalarCodec[Float] {
    def zero: Float = 0
    def wireType: Int = Wire.WireType.Fixed32
    def sizeNoTag(a: Float): Int = CodedOutputStream.computeFloatSizeNoTag(a)
    def sizeTag(field: Int, a: Float): Int = CodedOutputStream.computeFloatSize(field, a)
    def writeNoTag(a: Float, os: CodedOutputStream): Unit = os.writeFloatNoTag(a)
    def writeTag(field: Int, a: Float, os: CodedOutputStream): Unit = os.writeFloat(field, a)
    def read(is: CodedInputStream): Float = is.readFloat()
  }

}
