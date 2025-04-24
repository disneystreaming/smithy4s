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

import com.google.protobuf.CodedInputStream
import com.google.protobuf.CodedOutputStream

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
