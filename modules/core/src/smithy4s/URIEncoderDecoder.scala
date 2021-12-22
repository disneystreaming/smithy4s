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

import java.io.ByteArrayOutputStream
import java.net._

object URIEncoderDecoder {

  val digits: String = "0123456789ABCDEF"

  val encoding: String = "UTF8"

  def validate(s: String, legal: String): Unit = {
    var i: Int = 0
    while (i < s.length) {
      var continue = false
      val ch: Char = s.charAt(i)
      if (ch == '%') {
        continue = true
        while ({
          if (i + 2 >= s.length) {
            throw new URISyntaxException(s, "Incomplete % sequence", i)
          }
          val d1: Int = java.lang.Character.digit(s.charAt(i + 1), 16)
          val d2: Int = java.lang.Character.digit(s.charAt(i + 2), 16)
          if (d1 == -1 || d2 == -1) {
            throw new URISyntaxException(
              s,
              "Invalid % sequence (" + s.substring(i, i + 3) + ")",
              i
            )
          }
          i += 3
          // loop condition
          // Scala 3 dropped do-while loops
          // this is the recommended rewrite:
          // https://docs.scala-lang.org/scala3/reference/dropped-features/do-while.html
          (i < s.length && s.charAt(i) == '%')
        }) {}
      } else if (
        !((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') ||
          (ch >= '0' && ch <= '9') ||
          legal.indexOf(ch.toInt) > -1 ||
          (ch > 127 && !java.lang.Character.isSpaceChar(
            ch
          ) && !java.lang.Character
            .isISOControl(ch)))
      ) {
        throw new URISyntaxException(s, "Illegal character", i)
      }
      if (!continue) i += 1
    }
  }

  def validateSimple(s: String, legal: String): Unit = {
    var i: Int = 0
    while (i < s.length) {
      val ch: Char = s.charAt(i)
      if (
        !((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') ||
          (ch >= '0' && ch <= '9') ||
          legal.indexOf(ch.toInt) > -1)
      ) {
        throw new URISyntaxException(s, "Illegal character", i)
      }
      i += 1
    }
  }

  def quoteIllegal(s: String, legal: String): String = {
    val buf: StringBuilder = new StringBuilder()
    for (i <- 0 until s.length) {
      val ch: Char = s.charAt(i)
      if (
        (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') ||
        (ch >= '0' && ch <= '9') ||
        legal.indexOf(ch.toInt) > -1 ||
        (ch > 127 && !java.lang.Character.isSpaceChar(
          ch
        ) && !java.lang.Character
          .isISOControl(ch))
      ) {
        buf.append(ch)
      } else {
        val bytes: Array[Byte] = new String(Array(ch)).getBytes(encoding)
        for (j <- bytes.indices) {
          buf.append('%')
          buf.append(digits.charAt((bytes(j) & 0xf0) >> 4))
          buf.append(digits.charAt(bytes(j) & 0xf))
        }
      }
    }
    buf.toString
  }

  def encodeOthers(s: String): String = {
    val buf: StringBuilder = new StringBuilder()
    for (i <- 0 until s.length) {
      val ch: Char = s.charAt(i)
      if (ch <= 127) {
        buf.append(ch)
      } else {
        val bytes: Array[Byte] = new String(Array(ch)).getBytes(encoding)
        for (j <- bytes.indices) {
          buf.append('%')
          buf.append(digits.charAt((bytes(j) & 0xf0) >> 4))
          buf.append(digits.charAt(bytes(j) & 0xf))
        }
      }
    }
    buf.toString
  }

  def decode(s: String): String = {
    val result: StringBuilder = new StringBuilder()
    val out: ByteArrayOutputStream = new ByteArrayOutputStream()
    var i: Int = 0
    while (i < s.length) {
      val c: Char = s.charAt(i)
      if (c == '%') {
        out.reset()
        while ({
          if (i + 2 >= s.length) {
            throw new IllegalArgumentException("Incomplete % sequence at: " + i)
          }
          val d1: Int = java.lang.Character.digit(s.charAt(i + 1), 16)
          val d2: Int = java.lang.Character.digit(s.charAt(i + 2), 16)
          if (d1 == -1 || d2 == -1) {
            throw new IllegalArgumentException(
              "Invalid % sequence (" + s.substring(i, i + 3) + ") at: " + i
            )
          }
          out.write(((d1 << 4) + d2))
          i += 3
          // loop condition
          // Scala 3 dropped do-while loops
          // this is the recommended rewrite:
          // https://docs.scala-lang.org/scala3/reference/dropped-features/do-while.html
          (i < s.length && s.charAt(i) == '%')
        }) {}
        result.append(out.toString(encoding))
      }
      result.append(c)
      i += 1
    }
    result.toString
  }

}
