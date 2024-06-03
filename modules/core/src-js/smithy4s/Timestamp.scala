/*
 *  Copyright 2021-2024 Disney Streaming
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

import smithy.api.TimestampFormat
import scalajs.js.Date
import scala.util.control.{NoStackTrace, NonFatal}

case class Timestamp private (epochSecond: Long, nano: Int) {

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

  /** JS platform only method */
  def toDate: Date = {
    // The 0 there is the key, which sets the date to the epoch
    val date = new Date(0)
    date.setUTCSeconds(epochSecond.toDouble + (nano / 1000000000.0))
    date
  }

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
      val adjust400YearCycles = to400YearCycle(marchZeroDay + 1) - 1
      adjustYear = adjust400YearCycles * 400
      marchZeroDay -= adjust400YearCycles * 146097L
    }
    var year = to400YearCycle(marchZeroDay * 400 + 591)
    var marchDayOfYear = toMarchDayOfYear(marchZeroDay, year)
    if (marchDayOfYear < 0) { // fix year estimate
      year -= 1
      marchDayOfYear = toMarchDayOfYear(marchZeroDay, year)
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
        s.append(Timestamp.daysOfWeek(((epochDay + 700000003) % 7).toInt))
          .append(',')
        append2Digits(day, s.append(' '))
        s.append(' ').append(Timestamp.months(month - 1))
        append4Digits(year, s.append(' '))
        appendTime(secsOfDay, s.append(' '), addSeparator = true)
        appendNano(nano, s)
        s.append(" GMT").toString
      case 2 =>
        append4Digits(year, s)
        append2Digits(month, s)
        append2Digits(day, s)
        s.toString
      case 3 =>
        append4Digits(year, s)
        append2Digits(month, s)
        append2Digits(day, s)
        appendTime(secsOfDay, s.append('T'), addSeparator = false)
        s.append('Z').toString
      case _ =>
        append4Digits(year, s)
        append2Digits(month, s.append('-'))
        append2Digits(day, s.append('-'))
        appendTime(secsOfDay, s.append('T'), addSeparator = true)
        appendNano(nano, s)
        s.append('Z').toString
    }
  }

  private[this] def formatEpochSeconds: String = {
    val s = new java.lang.StringBuilder(32)
    s.append(epochSecond)
    appendNano(nano, s)
    s.toString
  }

  private[this] def appendTime(
      secsOfDay: Int,
      s: java.lang.StringBuilder,
      addSeparator: Boolean
  ): Unit = {
    val minutesOfDay = secsOfDay / 60
    val hour = minutesOfDay / 60
    val minute = minutesOfDay - hour * 60
    val second = secsOfDay - minutesOfDay * 60

    if (addSeparator) {
      append2Digits(hour, s)
      append2Digits(minute, s.append(':'))
      append2Digits(second, s.append(':'))
    } else {
      append2Digits(hour, s)
      append2Digits(minute, s)
      append2Digits(second, s)
    }
  }

  private[this] def appendNano(nano: Int, s: java.lang.StringBuilder): Unit =
    if (nano != 0) {
      s.append('.')
      val q1 = nano / 10000000
      val r1 = nano - q1 * 10000000
      append2Digits(q1, s)
      val q2 = r1 / 100000
      val r2 = r1 - q2 * 100000
      val d = Timestamp.digits(q2)
      s.append(d.toByte.toChar)
      if (r2 != 0 || d > 0x3039) { // check if nano is divisible by 1000000
        s.append((d >> 8).toByte.toChar)
        val q3 = r2 / 1000
        val r3 = r2 - q3 * 1000
        append2Digits(q3, s)
        if (r3 != 0) { // check if nano is divisible by 1000
          append3Digits(r3, s)
        }
      }
    }

  private[this] def append4Digits(x: Int, s: java.lang.StringBuilder): Unit = {
    val q = x * 5243 >> 19 // divide a 4-digit positive int by 100
    append2Digits(q, s)
    append2Digits(x - q * 100, s)
  }

  private[this] def append3Digits(x: Int, s: java.lang.StringBuilder): Unit = {
    val q = x * 5243 >> 19 // divide a 4-digit positive int by 100
    s.append((q + '0').toChar)
    append2Digits(x - q * 100, s)
  }

  private[this] def append2Digits(x: Int, s: java.lang.StringBuilder): Unit = {
    val d = Timestamp.digits(x)
    val _ = s.append((d & 0xff).toChar).append((d >> 8).toChar)
  }

  private[this] def to400YearCycle(day: Long): Int =
    (day / 146097).toInt // 146097 == number of days in a 400 year cycle

  private[this] def toMarchDayOfYear(marchZeroDay: Long, year: Int): Int = {
    val century = year / 100
    (marchZeroDay - year * 365L).toInt - (year >> 2) + century - (century >> 2)
  }
}

object Timestamp {

  val epoch = Timestamp(0, 0)

  private val digits: Array[Short] = Array(
    0x3030, 0x3130, 0x3230, 0x3330, 0x3430, 0x3530, 0x3630, 0x3730, 0x3830,
    0x3930, 0x3031, 0x3131, 0x3231, 0x3331, 0x3431, 0x3531, 0x3631, 0x3731,
    0x3831, 0x3931, 0x3032, 0x3132, 0x3232, 0x3332, 0x3432, 0x3532, 0x3632,
    0x3732, 0x3832, 0x3932, 0x3033, 0x3133, 0x3233, 0x3333, 0x3433, 0x3533,
    0x3633, 0x3733, 0x3833, 0x3933, 0x3034, 0x3134, 0x3234, 0x3334, 0x3434,
    0x3534, 0x3634, 0x3734, 0x3834, 0x3934, 0x3035, 0x3135, 0x3235, 0x3335,
    0x3435, 0x3535, 0x3635, 0x3735, 0x3835, 0x3935, 0x3036, 0x3136, 0x3236,
    0x3336, 0x3436, 0x3536, 0x3636, 0x3736, 0x3836, 0x3936, 0x3037, 0x3137,
    0x3237, 0x3337, 0x3437, 0x3537, 0x3637, 0x3737, 0x3837, 0x3937, 0x3038,
    0x3138, 0x3238, 0x3338, 0x3438, 0x3538, 0x3638, 0x3738, 0x3838, 0x3938,
    0x3039, 0x3139, 0x3239, 0x3339, 0x3439, 0x3539, 0x3639, 0x3739, 0x3839,
    0x3939
  )
  private val daysOfWeek: Array[String] =
    Array("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
  private val months: Array[String] =
    Array(
      "Jan",
      "Feb",
      "Mar",
      "Apr",
      "May",
      "Jun",
      "Jul",
      "Aug",
      "Sep",
      "Oct",
      "Nov",
      "Dec"
    )

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
      day >= 1 && (day <= 28 || day <= maxDayForYearMonth(year, month)),
      "illegal year, month, day combination"
    )
    require(hour >= 0 && hour <= 23, "illegal hour")
    require(minute >= 0 && minute <= 59, "illegal minute")
    require(second >= 0 && second <= 59, "illegal second")
    require(nano >= 0 && nano <= 999999999, "illegal nano")
    new Timestamp(
      toEpochDay(
        year,
        month,
        day
      ) * 86400 + (hour * 3600 + minute * 60 + second),
      nano
    )
  }

  def fromEpochSecond(epochSecond: Long): Timestamp = Timestamp(epochSecond, 0)
  def fromEpochMilli(epochMilli: Long): Timestamp = {
    Timestamp(
      (epochMilli / 1000),
      (epochMilli % 1000).toInt * 1000000
    )
  }

  /** JS platform only method */
  def fromDate(x: Date): Timestamp = fromEpochMilli(x.valueOf().toLong)

  def nowUTC(): Timestamp = fromDate(new Date())

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
        (day > 28 && day > maxDayForYearMonth(year, month)) || ch2 != 'T'
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
    var epochSecond = toEpochDay(
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
          val d = daysOfWeek(i)
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
          val m = months(i)
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
    if (day > 28 && day > maxDayForYearMonth(year, month)) error()
    val epochDay = toEpochDay(year, month, day)
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

  private[this] def toEpochDay(year: Int, month: Int, day: Int): Long =
    year * 365L + (((year + 3) >> 2) - {
      if (year < 0) year / 100 - year / 400
      else (year + 99) / 100 - (year + 399) / 400
    } + ((month * 1002277 - 988622) >> 15) - // (month * 367 - 362) / 12
      (if (month <= 2) 0
       else if (isLeap(year)) 1
       else 2) + day - 719529) // 719528 == days 0000 to 1970

  private[this] def maxDayForYearMonth(year: Int, month: Int): Int =
    if (month != 2) ((month >> 3) ^ (month & 0x1)) + 30
    else if (isLeap(year)) 29
    else 28

  private[this] def isLeap(year: Int): Boolean =
    (year & 0x3) == 0 && (year % 100 != 0 || year % 400 == 0)

  private[this] def error(): Throwable = throw new RuntimeException
    with NoStackTrace
}
