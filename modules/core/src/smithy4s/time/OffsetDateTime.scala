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

package smithy4s.time

import scala.util.control.NoStackTrace
import scala.util.control.NonFatal

case class OffsetDateTime private (timestamp: Timestamp, offset: ZoneOffset) {
  override def toString: String = {
    val s = new java.lang.StringBuilder(32)
    val epochSecond = timestamp.epochSecond
    val nano = timestamp.nano
    val epochDay =
      (if (epochSecond >= 0) epochSecond
       else epochSecond - 86399) / 86400 // 86400 == seconds per day
    val secsOfDay = (epochSecond - epochDay * 86400).toInt
    var marchZeroDay =
      epochDay + 719468 // 719468 == 719528 - 60 == days 0000 to 1970 - days 1st Jan to 1st Mar
    var adjustYear = 0
    if (marchZeroDay < 0) { // adjust negative years to positive for calculation
      val adjust400YearCycles = TimeUtil.to400YearCycle(marchZeroDay + 1) - 1
      adjustYear = adjust400YearCycles * 400
      marchZeroDay -= adjust400YearCycles * 146097L
    }
    var year = TimeUtil.to400YearCycle(marchZeroDay * 400 + 591)
    var marchDayOfYear = TimeUtil.toMarchDayOfYear(marchZeroDay, year)
    if (marchDayOfYear < 0) { // fix year estimate
      year -= 1
      marchDayOfYear = TimeUtil.toMarchDayOfYear(marchZeroDay, year)
    }
    val marchMonth =
      (marchDayOfYear * 17135 + 6854) >> 19 // (marchDayOfYear * 5 + 2) / 153
    year += (marchMonth * 3277 >> 15) + adjustYear // year += marchMonth / 10 + adjustYear (reset any negative year and convert march-based values back to january-based)
    val month = marchMonth +
      (if (marchMonth < 10) 3
       else -9)
    val day =
      marchDayOfYear - ((marchMonth * 1002762 - 16383) >> 15) // marchDayOfYear - (marchMonth * 306 + 5) / 10 + 1

    TimeUtil.append4Digits(year, s)
    TimeUtil.append2Digits(month, s.append('-'))
    TimeUtil.append2Digits(day, s.append('-'))
    appendTime(secsOfDay, s.append('T'), addSeparator = true)
    appendNano(nano, s)
    if (offset.seconds == 0) {
      s.append('Z')
    } else {
      s.append(offset.toString())
    }

    val result = s.toString
    result
  }

  private[this] def appendTime(
      secsOfDay: Int,
      s: java.lang.StringBuilder,
      addSeparator: Boolean
  ): Unit = {
    val y1 =
      secsOfDay * 1193047L // Based on James Anhalt's algorithm: https://jk-jeon.github.io/posts/2022/02/jeaiii-algorithm/
    val y2 = (y1 & 0xffffffffL) * 60
    val y3 = (y2 & 0xffffffffL) * 60

    if (addSeparator) {
      TimeUtil.append2Digits((y1 >> 32).toInt, s)
      TimeUtil.append2Digits((y2 >> 32).toInt, s.append(':'))
      TimeUtil.append2Digits((y3 >> 32).toInt, s.append(':'))
    } else {
      TimeUtil.append2Digits((y1 >> 32).toInt, s)
      TimeUtil.append2Digits((y2 >> 32).toInt, s)
      TimeUtil.append2Digits((y3 >> 32).toInt, s)
    }
  }

  private[this] def appendNano(nano: Int, s: java.lang.StringBuilder): Unit =
    if (nano != 0) {
      val y1 =
        nano * 1441151881L // Based on James Anhalt's algorithm for 9 digits: https://jk-jeon.github.io/posts/2022/02/jeaiii-algorithm/
      val y2 = (y1 & 0x1ffffffffffffffL) * 100
      s.append('.').append(((y1 >>> 57).toInt + '0').toChar)
      TimeUtil.append2Digits((y2 >>> 57).toInt, s)
      if ((y2 & 0x1fffff800000000L) != 0) { // check if nano is divisible by 1000000
        val y3 = (y2 & 0x1ffffffffffffffL) * 100
        val y4 = (y3 & 0x1ffffffffffffffL) * 100
        TimeUtil.append2Digits((y3 >>> 57).toInt, s)
        val d = TimeUtil.digits((y4 >>> 57).toInt)
        s.append((d & 0xff).toChar)
        if ((y4 & 0x1ff000000000000L) != 0 || d > 0x3039) { // check if nano is divisible by 1000
          TimeUtil.append2Digits(
            ((y4 & 0x1ffffffffffffffL) * 100 >>> 57).toInt,
            s.append((d >> 8).toChar)
          )
        }
      }
    }
}

object OffsetDateTime extends OffsetDateTimeCompanionPlatform {

  val epoch = OffsetDateTime(Timestamp.epoch, ZoneOffset.Zero)

  def fromEpochMilli(epochMilli: Long, offset: ZoneOffset): OffsetDateTime = {
    val secs = java.lang.Math.floorDiv(epochMilli, 1000)
    val mos = java.lang.Math.floorMod(epochMilli, 1000)
    OffsetDateTime(Timestamp(secs, (mos * 1000000).toInt), offset)
  }

  def apply(seconds: Long, nano: Int, offset: ZoneOffset): OffsetDateTime = {
    OffsetDateTime(Timestamp(seconds, nano), offset)
  }

  def apply(
      year: Int,
      month: Int,
      day: Int,
      hour: Int = 0,
      minute: Int = 0,
      second: Int = 0,
      nano: Int = 0,
      offset: ZoneOffset = ZoneOffset.Zero
  ): OffsetDateTime = {
    require(year >= 0 && year <= 9999, "illegal year")
    require(month >= 1 && month <= 12, "illegal month")
    require(
      day >= 1 && (day <= 28 || day <= TimeUtil
        .maxDayForYearMonth(year, month)),
      "illegal year, month, day combination"
    )
    require(hour >= 0 && hour <= 23, "illegal hour")
    require(minute >= 0 && minute <= 59, "illegal minute")
    require(second >= 0 && second <= 59, "illegal second")
    require(nano >= 0 && nano <= 999999999, "illegal nano")
    require(offset.toTotalHours <= 18, "illegal offset")

    val timestamp = Timestamp(
      TimeUtil.toEpochDay(
        year,
        month,
        day
      ) * 86400 + (hour * 3600 + minute * 60 + second),
      nano
    )
    new OffsetDateTime(timestamp, offset)
  }

  def parse(string: String): Option[OffsetDateTime] = try {
    Some(parseUnsafe(string))
  } catch {
    case NonFatal(_) => None
  }

  def parseUnsafe(s: String): OffsetDateTime = {
    val len = s.length
    if (len < 16) error()
    var pos = 0
    val year = {
      val ch0 = s.charAt(pos)
      val ch1 = s.charAt(pos + 1)
      val ch2 = s.charAt(pos + 2)
      val ch3 = s.charAt(pos + 3)
      val ch4 = s.charAt(pos + 4)
      if (
        ch0 < '0' || ch0 > '9' || ch1 < '0' || ch1 > '9' || ch2 < '0' || ch2 > '9' || ch3 < '0' || ch3 > '9'
        || ch4 != '-'
      ) error()
      pos += 5
      ch0 * 1000 + ch1 * 100 + ch2 * 10 + ch3 - 53328 // 53328 == '0' * 1111
    }
    val month = {
      val ch0 = s.charAt(pos)
      val ch1 = s.charAt(pos + 1)
      val ch2 = s.charAt(pos + 2)
      val month = ch0 * 10 + ch1 - 528 // 528 == '0' * 11
      if (
        ch0 < '0' || ch0 > '1' || ch1 < '0' || ch1 > '9' || month < 1 || month > 12 || ch2 != '-'
      ) error()
      pos += 3
      month
    }
    val day = {
      val ch0 = s.charAt(pos)
      val ch1 = s.charAt(pos + 1)
      val ch2 = s.charAt(pos + 2)
      val day = ch0 * 10 + ch1 - 528 // 528 == '0' * 11
      if (
        ch0 < '0' || ch0 > '3' || ch1 < '0' || ch1 > '9' || day == 0 ||
        (day > 28 && day > TimeUtil.maxDayForYearMonth(
          year,
          month
        )) || ch2 != 'T'
      ) error()
      pos += 3
      day
    }
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
    val epochSecond = TimeUtil.toEpochDay(
      year,
      month,
      day
    ) * 86400 + (hour * 3600 + minute * 60 + second)
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
    var offset = 0
    if (ch != 'Z') {
      val isNeg = ch == '-' || (ch != '+' && {
        error()
        true
      })
      if (pos + 2 > len) error()
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

      offset = offsetTotal
    }
    if (pos != len) error()

    val timestamp = Timestamp(epochSecond, nano)

    new OffsetDateTime(timestamp, ZoneOffset(offset))
  }

  private[this] def error(): Throwable = throw new RuntimeException
    with NoStackTrace
}
