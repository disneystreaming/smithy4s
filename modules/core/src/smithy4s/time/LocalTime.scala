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

case class LocalTime private (seconds: Int, nano: Int)
    extends LocalTimePlatform {

  def isAfter(other: LocalTime): Boolean = {
    val diff = seconds - other.seconds
    diff > 0 || diff == 0 && nano > other.nano
  }

  override def toString: String = {
    val s = new java.lang.StringBuilder(32)

    val minutesOfDay = seconds / 60
    val hour = minutesOfDay / 60
    val minute = minutesOfDay - hour * 60
    val second = seconds - minutesOfDay * 60

    TimeUtil.append2Digits(hour, s)
    TimeUtil.append2Digits(minute, s.append(':'))
    TimeUtil.append2Digits(second, s.append(':'))
    TimeUtil.appendNano(nano, s)
    s.toString
  }

  def toNanoOfDay: Long = {
    seconds * 1000000000L + nano
  }

}

object LocalTime extends LocalTimeCompanionPlatform {

  val midnight = LocalTime(0, 0)
  val noon = LocalTime(60 * 60 * 12, 0)
  val max = LocalTime(86399, 999999999)

  def apply(seconds: Int, nano: Int): LocalTime = {
    require(
      seconds >= 0 && seconds <= 86399,
      "illegal second"
    )
    require(nano >= 0 && nano <= 999999999, "illegal nano")
    new LocalTime(seconds, nano)
  }

  def apply(hour: Int, minute: Int, second: Int, nano: Int = 0): LocalTime = {
    require(hour >= 0 && hour <= 23, "illegal hour")
    require(minute >= 0 && minute <= 59, "illegal minute")
    require(second >= 0 && second <= 59, "illegal second")
    require(nano >= 0 && nano <= 999999999, "illegal nano")

    val totalSeconds = hour * 3600 + minute * 60 + second
    LocalTime(totalSeconds, nano)
  }

  def fromSeconds(seconds: Int): LocalTime = LocalTime(seconds, 0)

  def parse(string: String): Option[LocalTime] = try {
    Some(parseUnsafe(string))
  } catch {
    case NonFatal(_) => None
  }

  def parseUnsafe(s: String): LocalTime = {
    val len = s.length
    if (len < 8) error()
    var pos = 0

    val hour = {
      val ch0 = s.charAt(pos)
      val ch1 = s.charAt(pos + 1)
      val ch2 = s.charAt(pos + 2)
      val hour = ch0 * 10 + ch1 - 528 // 528 == '0' * 11
      if (
        ch0 < '0' || ch0 > '2' || ch1 < '0' || ch1 > '9' || hour > 23 || ch2 != ':'
      ) error()
      pos += 3
      hour
    }

    val minute = {
      val ch0 = s.charAt(pos)
      val ch1 = s.charAt(pos + 1)
      if (ch0 < '0' || ch0 > '5' || ch1 < '0' || ch1 > '9')
        error()
      pos += 2
      ch0 * 10 + ch1 - 528 // 528 == '0' * 11
    }

    val second = {
      val separator = s.charAt(pos)
      if (separator == ':') {
        val ch0 = s.charAt(pos + 1)
        val ch1 = s.charAt(pos + 2)
        if (ch0 < '0' || ch0 > '5' || ch1 < '0' || ch1 > '9') error()
        pos += 3
        ch0 * 10 + ch1 - 528 // 528 == '0' * 11
      } else 0
    }

    val totalSeconds = hour * 3600 + minute * 60 + second

    var nano = 0
    var ch = (0: Char)
    if (pos < len) {
      ch = s.charAt(pos)
      pos += 1
      if (ch == '.') {
        var nanoDigitWeight = 100000000
        while (
          pos < len && {
            ch = s.charAt(pos)
            pos += 1
            (ch >= '0' && ch <= '9') && nanoDigitWeight != 0
          }
        ) {
          nano += (ch - '0') * nanoDigitWeight
          nanoDigitWeight =
            (nanoDigitWeight * 429496730L >> 32).toInt // divide a small positive int by 10
        }
      }
    }
    if (pos != len) error()
    new LocalTime(totalSeconds, nano)
  }

  private[this] def error(): Throwable = throw new RuntimeException
    with NoStackTrace
}
