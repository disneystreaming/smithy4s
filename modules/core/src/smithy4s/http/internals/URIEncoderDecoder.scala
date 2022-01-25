/*
 *  Copyright 2021 Disney Streaming
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

package smithy4s
package http
package internals

import scala.annotation.tailrec

private[smithy4s] object URIEncoderDecoder {

  val digits: String = "0123456789ABCDEF"
  val encoding: String = "UTF8"

  def encode(s: String): String = {
    if (s == null) {
      throw new NullPointerException
    }
    val buf = new java.lang.StringBuilder(s.length + 16)
    var start = -1
    @tailrec
    def loop(i: Int): Unit = {
      if (i < s.length) {
        val ch: Char = s.charAt(i)
        if (
          (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9') || " .-*_"
            .indexOf(ch.toInt) > -1
        ) {
          if (start >= 0) {
            encodeOthers(s.substring(start, i), buf, encoding)
            start = -1
          }
          if (ch != ' ') {
            buf.append(ch)
          } else {
            buf.append('+')
          }
        } else if (start < 0) {
          start = i
        }
        loop(i + 1)
      }
    }
    loop(0)

    if (start >= 0) {
      encodeOthers(s.substring(start, s.length), buf, encoding)
    }
    buf.toString
  }

  private def encodeOthers(
      s: String,
      buf: java.lang.StringBuilder,
      enc: String
  ): Unit = {
    val bytes = s.getBytes(enc)
    @tailrec
    def loop(j: Int): Unit = {
      if (j < bytes.length) {
        buf.append('%')
        buf.append(digits((bytes(j) & 0xf0) >> 4))
        buf.append(digits(bytes(j) & 0xf))
        loop(j + 1)
      }
    }
    loop(0)
  }

}
