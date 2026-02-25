/*
 *  Copyright 2021-2026 Disney Streaming
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

package smithy4s.grpc

import java.nio.charset.StandardCharsets
import scala.collection.mutable.ArrayBuilder

object GrpcMessageEncoding {

  sealed trait DecodeError extends Product with Serializable {
    def message: String
  }

  object DecodeError {
    final case class InvalidPercentEncoding(value: String, index: Int)
        extends DecodeError {
      val message: String = s"Invalid percent-encoding at index $index in '$value'"
    }

    final case class InvalidAscii(value: String, index: Int) extends DecodeError {
      val message: String = s"Invalid ASCII character at index $index in '$value'"
    }
  }

  def encode(value: String): String = {
    val bytes = value.getBytes(StandardCharsets.UTF_8)
    val builder = new StringBuilder(bytes.length)
    var i = 0
    while (i < bytes.length) {
      val b = bytes(i) & 0xff
      if (isUnreserved(b)) builder.append(b.toChar)
      else {
        builder.append('%')
        builder.append(toHexUpper(b >>> 4))
        builder.append(toHexUpper(b & 0x0f))
      }
      i += 1
    }
    builder.toString
  }

  def decode(value: String): Either[DecodeError, String] = {
    val builder = ArrayBuilder.make[Byte]

    @annotation.tailrec
    def loop(i: Int): Either[DecodeError, String] = {
      if (i >= value.length) {
        Right(new String(builder.result(), StandardCharsets.UTF_8))
      } else {
        val ch = value.charAt(i)
        if (ch == '%') {
          if (i + 2 >= value.length) {
            Left(DecodeError.InvalidPercentEncoding(value, i))
          } else {
            val hi = fromHex(value.charAt(i + 1))
            val lo = fromHex(value.charAt(i + 2))
            if (hi < 0 || lo < 0) {
              Left(DecodeError.InvalidPercentEncoding(value, i))
            } else {
              builder += ((hi << 4) + lo).toByte
              loop(i + 3)
            }
          }
        } else if (ch <= 0x7f) {
          builder += ch.toByte
          loop(i + 1)
        } else {
          Left(DecodeError.InvalidAscii(value, i))
        }
      }
    }

    loop(0)
  }

  // gRPC Percent-Byte-Unencoded = %x20-%x24 / %x26-%x7E (space and VCHAR, except '%').
  private def isUnreserved(b: Int): Boolean =
    (b >= 0x20 && b <= 0x24) || (b >= 0x26 && b <= 0x7e)

  private def toHexUpper(n: Int): Char =
    if (n < 10) (n + '0').toChar else (n - 10 + 'A').toChar

  private def fromHex(c: Char): Int = {
    if (c >= '0' && c <= '9') c - '0'
    else if (c >= 'A' && c <= 'F') c - 'A' + 10
    else if (c >= 'a' && c <= 'f') c - 'a' + 10
    else -1
  }
}
