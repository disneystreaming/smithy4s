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

private[internals] object Wire {

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
    final val Varint = 0x00
    final val Fixed64 = 0x01
    final val LengthDelimited = 0x02
    final val Fixed32 = 0x05
  }

}
