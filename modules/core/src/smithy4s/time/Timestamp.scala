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

import smithy.api.TimestampFormat

import scala.util.control.NoStackTrace
import scala.util.control.NonFatal

case class Timestamp private (epochSecond: Long, nano: Int)
    extends TimestampPlatform {

  def epochMilli: Long = epochSecond * 1000 + nano / 1000000

  def isAfter(other: Timestamp): Boolean = {
    val diff = epochSecond - other.epochSecond
    diff > 0 || diff == 0 && nano > other.nano
  }

  def format(format: TimestampFormat): String = format match {
    case TimestampFormat.DATE_TIME     => formatToString(0)
    case TimestampFormat.EPOCH_SECONDS => formatEpochSeconds
    case TimestampFormat.HTTP_DATE     => formatToString(1)
  }

  def conciseDateTime: String = formatToString(3)

  def conciseDate: String = formatToString(2)

  /**
    * @return a copy of this timestamp truncated to a miliseconds precision
    */
  def truncateToMillis: Timestamp = copy(nano = (nano / 1000000) * 1000000)

  /**
    * @return a copy of this timestamp truncated to a seconds resolution
    */
  def truncateToSeconds: Timestamp = copy(nano = 0)

  override def toString: String = format(TimestampFormat.DATE_TIME)

  private[this] def formatToString(internalFormat: Int): String = {
    val s = new java.lang.StringBuilder(32)
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
    internalFormat match {
      case 1 =>
        s.append(TimeUtil.daysOfWeek(((epochDay + 700000003) % 7).toInt))
          .append(',')
        TimeUtil.append2Digits(day, s.append(' '))
        s.append(' ').append(TimeUtil.months(month - 1))
        TimeUtil.append4Digits(year, s.append(' '))
        appendTime(secsOfDay, s.append(' '), addSeparator = true)
        TimeUtil.appendNano(nano, s)
        s.append(" GMT").toString
      case 2 =>
        TimeUtil.append4Digits(year, s)
        TimeUtil.append2Digits(month, s)
        TimeUtil.append2Digits(day, s)
        s.toString
      case 3 =>
        TimeUtil.append4Digits(year, s)
        TimeUtil.append2Digits(month, s)
        TimeUtil.append2Digits(day, s)
        appendTime(secsOfDay, s.append('T'), addSeparator = false)
        s.append('Z').toString
      case _ =>
        TimeUtil.append4Digits(year, s)
        TimeUtil.append2Digits(month, s.append('-'))
        TimeUtil.append2Digits(day, s.append('-'))
        appendTime(secsOfDay, s.append('T'), addSeparator = true)
        TimeUtil.appendNano(nano, s)
        s.append('Z').toString
    }
  }

  private[this] def formatEpochSeconds: String = {
    val s = new java.lang.StringBuilder(32)
    s.append(epochSecond)
    TimeUtil.appendNano(nano, s)
    s.toString
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

}

object Timestamp extends TimestampCompanionPlatform {

  val epoch = Timestamp(0, 0)

  def fromEpochMilli(epochMilli: Long): Timestamp = {
    val secs = java.lang.Math.floorDiv(epochMilli, 1000)
    val mos = java.lang.Math.floorMod(epochMilli, 1000)
    Timestamp(secs, (mos * 1000000).toInt)
  }

  def apply(epochSecond: Long, nano: Int): Timestamp = {
    require(
      epochSecond >= -62167219200L && epochSecond <= 253402300799L,
      "illegal epochSecond"
    )
    require(nano >= 0 && nano <= 999999999, "illegal nano")
    new Timestamp(epochSecond, nano)
  }

  def apply(
      year: Int,
      month: Int,
      day: Int,
      hour: Int = 0,
      minute: Int = 0,
      second: Int = 0,
      nano: Int = 0
  ): Timestamp = {
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
    new Timestamp(
      TimeUtil.toEpochDay(
        year,
        month,
        day
      ) * 86400 + (hour * 3600 + minute * 60 + second),
      nano
    )
  }

  def fromEpochSecond(epochSecond: Long): Timestamp = Timestamp(epochSecond, 0)

  def parse(string: String, format: TimestampFormat): Option[Timestamp] = try {
    new Some(format match {
      case TimestampFormat.DATE_TIME     => parseDateTime(string)
      case TimestampFormat.EPOCH_SECONDS => parseEpochSeconds(string)
      case TimestampFormat.HTTP_DATE     => parseHTTPDate(string)
    })
  } catch {
    case NonFatal(_) => None
  }

  def showFormat(format: TimestampFormat): String = format match {
    case TimestampFormat.DATE_TIME =>
      "date-time timestamp (YYYY-MM-ddThh:mm:ss.sssZ)"
    case TimestampFormat.EPOCH_SECONDS => "epoch-second timestamp"
    case TimestampFormat.HTTP_DATE =>
      "http-date timestamp (EEE, dd MMM yyyy hh:mm:ss.sss z)"
  }

  private[this] def parseDateTime(s: String): Timestamp = {
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
    var epochSecond = TimeUtil.toEpochDay(
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
      epochSecond -= offsetTotal
    }
    if (pos != len) error()
    new Timestamp(epochSecond, nano)
  }

  private[this] def parseEpochSeconds(s: String): Timestamp = {
    val len = s.length
    if (len == 0) error()
    var pos = 0
    var ch = s.charAt(pos)
    pos += 1
    val isNeg = ch == '-'
    if (isNeg) {
      ch = s.charAt(pos)
      pos += 1
    }
    if (ch < '0' || ch > '9') error()
    var epochSecond: Long = (ch - '0').toLong
    while (
      pos < len && {
        ch = s.charAt(pos)
        ch >= '0' && ch <= '9'
      }
    ) {
      epochSecond = epochSecond * 10 + (ch - '0')
      if (epochSecond > 377705116800L) error()
      pos += 1
    }
    if (isNeg) epochSecond = -epochSecond
    if (epochSecond > 253402300799L) error()
    var nano = 0
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
    new Timestamp(epochSecond, nano)

  }

  private[this] def parseHTTPDate(s: String): Timestamp = {
    val len = s.length
    if (len < 29) error()
    var pos = 0
    val dayOfWeek = {
      val ch0 = s.charAt(pos)
      val ch1 = s.charAt(pos + 1)
      val ch2 = s.charAt(pos + 2)
      val ch3 = s.charAt(pos + 3)
      val ch4 = s.charAt(pos + 4)
      var i = 0
      while (
        i < 7 && {
          val d = TimeUtil.daysOfWeek(i)
          d.charAt(0) != ch0 || d.charAt(1) != ch1 || d.charAt(2) != ch2
        }
      ) i += 1
      val dayOfWeek = i + 1
      if (dayOfWeek > 7 || ch3 != ',' || ch4 != ' ') error()
      pos += 5
      dayOfWeek
    }
    val day = {
      val ch0 = s.charAt(pos)
      val ch1 = s.charAt(pos + 1)
      val ch2 = s.charAt(pos + 2)
      val day = ch0 * 10 + ch1 - 528 // 528 == '0' * 11
      if (
        ch0 < '0' || ch0 > '3' || ch1 < '0' || ch1 > '9' || day == 0 || day > 31 || ch2 != ' '
      ) error()
      pos += 3
      day
    }
    val month = {
      val ch0 = s.charAt(pos)
      val ch1 = s.charAt(pos + 1)
      val ch2 = s.charAt(pos + 2)
      val ch3 = s.charAt(pos + 3)
      var i = 0
      while (
        i < 12 && {
          val m = TimeUtil.months(i)
          m.charAt(0) != ch0 || m.charAt(1) != ch1 || m.charAt(2) != ch2
        }
      ) i += 1
      val month = i + 1
      if (month > 12 || ch3 != ' ') error()
      pos += 4
      month
    }
    val year = {
      val ch0 = s.charAt(pos)
      val ch1 = s.charAt(pos + 1)
      val ch2 = s.charAt(pos + 2)
      val ch3 = s.charAt(pos + 3)
      val ch4 = s.charAt(pos + 4)
      if (
        ch0 < '0' || ch0 > '9' || ch1 < '0' || ch1 > '9' || ch2 < '0' || ch2 > '9' || ch3 < '0' || ch3 > '9'
        || ch4 != ' '
      ) error()
      pos += 5
      ch0 * 1000 + ch1 * 100 + ch2 * 10 + ch3 - 53328 // 53328 == '0' * 1111
    }
    if (day > 28 && day > TimeUtil.maxDayForYearMonth(year, month)) error()
    val epochDay = TimeUtil.toEpochDay(year, month, day)
    if (dayOfWeek != (epochDay + 700000003) % 7 + 1) error()
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
      val ch2 = s.charAt(pos + 2)
      if (ch0 < '0' || ch0 > '5' || ch1 < '0' || ch1 > '9' || ch2 != ':')
        error()
      pos += 3
      ch0 * 10 + ch1 - 528 // 528 == '0' * 11
    }
    val second = {
      val ch0 = s.charAt(pos)
      val ch1 = s.charAt(pos + 1)
      if (ch0 < '0' || ch0 > '5' || ch1 < '0' || ch1 > '9') error()
      pos += 2
      ch0 * 10 + ch1 - 528 // 528 == '0' * 11
    }
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
    if (
      ch != ' ' || pos + 2 >= len || {
        val ch0 = s.charAt(pos)
        val ch1 = s.charAt(pos + 1)
        val ch2 = s.charAt(pos + 2)
        pos += 3
        ch0 != 'G' || ch1 != 'M' || ch2 != 'T' || pos != len
      }
    ) error()
    new Timestamp(epochDay * 86400 + (hour * 3600 + minute * 60 + second), nano)
  }

  private[this] def error(): Throwable = throw new RuntimeException
    with NoStackTrace
}
