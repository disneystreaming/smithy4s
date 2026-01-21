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

package smithy4s.time

import scala.util.control.NoStackTrace
import scala.util.control.NonFatal

case class ZoneOffset private (seconds: Int) {
  def toTotalHours: Int = Math.abs(seconds) / 3600

  override def toString: String = {
    val s = new java.lang.StringBuilder(32)

    val sign = if (seconds < 0) '-' else '+'
    val hours =
      Math.abs(
        seconds
      ) * 1193047L // Based on James Anhalt's algorithm: https://jk-jeon.github.io/posts/2022/02/jeaiii-algorithm/
    val minutes = (hours & 0xffffffffL) * 60

    s.append(sign)
    TimeUtil.append2Digits((hours >> 32).toInt, s)
    TimeUtil.append2Digits((minutes >> 32).toInt, s.append(':'))

    s.toString()
  }
}

object ZoneOffset {

  val Zero = ZoneOffset(0)

  def apply(seconds: Int): ZoneOffset = new ZoneOffset(seconds)

  def hours(hours: Int): ZoneOffset = ZoneOffset(hours * 3600)

  def minutes(minutes: Int): ZoneOffset = ZoneOffset(minutes * 60)

  def parse(string: String): Option[ZoneOffset] = try {
    Some(parseUnsafe(string))
  } catch {
    case NonFatal(_) => None
  }

  def parseUnsafe(s: String): ZoneOffset = {
    val len = s.length
    var pos = 0
    var ch = s.charAt(pos)

    val isNeg = ch == '-' || (ch != '+' && {
      error()
      true
    })

    if (pos + 2 > len) error()

    pos += 1
    var offsetTotal = {
      val ch0 = s.charAt(pos)
      val ch1 = s.charAt(pos + 1)
      if (ch0 < '0' || ch0 > '1' || ch1 < '0' || ch1 > '9') error()
      pos += 2
      ch0 * 10 + ch1 - 528 // 528 == '0' * 11
    } * 3600

    if (
      pos + 3 <= len && {
        ch = s.charAt(pos)
        pos += 1
        ch == ':'
      } && {
        offsetTotal += {
          val ch0 = s.charAt(pos)
          val ch1 = s.charAt(pos + 1)
          if (ch0 < '0' || ch0 > '5' || ch1 < '0' || ch1 > '9') error()
          pos += 2
          ch0 * 10 + ch1 - 528 // 528 == '0' * 11
        } * 60
        pos + 3 <= len
      } && {
        ch = s.charAt(pos)
        pos += 1
        ch == ':'
      }
    ) offsetTotal += {
      val ch0 = s.charAt(pos)
      val ch1 = s.charAt(pos + 1)
      if (ch0 < '0' || ch0 > '5' || ch1 < '0' || ch1 > '9') error()
      pos += 2
      ch0 * 10 + ch1 - 528 // 528 == '0' * 11
    }
    if (offsetTotal > 64800) error() // 64800 == 18 * 60 * 60
    if (isNeg) offsetTotal = -offsetTotal

    ZoneOffset(offsetTotal)
  }

  private[this] def error(): Throwable = throw new RuntimeException
    with NoStackTrace
}
