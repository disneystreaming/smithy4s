/*
 *  Copyright 2021-2022 Disney Streaming
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
import smithy4s.Timestamp._
import java.time._
import scala.util.control.NoStackTrace

case class Timestamp private(epochSecond: Long, nano: Int) {
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

  /** JVM platform only method */
  def toInstant: Instant = Instant.ofEpochSecond(epochSecond, nano.toLong)

  /** JVM platform only method */
  def toOffsetDateTime: OffsetDateTime =
    OffsetDateTime.ofInstant(Instant.ofEpochSecond(epochSecond, nano.toLong), ZoneOffset.UTC)

  override def toString: String = format(TimestampFormat.DATE_TIME)

  private[this] def formatToString(internalFormat: Int): String = {
    val s = new java.lang.StringBuilder(32)
    val epochDay =
      (if (epochSecond >= 0) epochSecond
      else epochSecond - 86399) / 86400 // 86400 == seconds per day
    val secsOfDay = (epochSecond - epochDay * 86400).toInt
    var marchZeroDay = epochDay + 719468 // 719468 == 719528 - 60 == days 0000 to 1970 - days 1st Jan to 1st Mar
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
    val marchMonth = (marchDayOfYear * 17135 + 6854) >> 19 // (marchDayOfYear * 5 + 2) / 153
    year += (marchMonth * 3277 >> 15) + adjustYear // year += marchMonth / 10 + adjustYear (reset any negative year and convert march-based values back to january-based)
    val month = marchMonth +
      (if (marchMonth < 10) 3
      else -9)
    val day = marchDayOfYear - ((marchMonth * 1002762 - 16383) >> 15) // marchDayOfYear - (marchMonth * 306 + 5) / 10 + 1
    internalFormat match {
      case 1 =>
        s.append(daysOfWeek(((epochDay + 700000003) % 7).toInt)).append(',')
        append2Digits(day, s.append(' '))
        s.append(' ').append(months(month - 1))
        append4Digits(year, s.append(' '))
        appendTime(secsOfDay, s.append(' '))
        appendNano(nano, s)
        s.append(" GMT").toString
      case 2 =>
        append4Digits(year, s)
        append2Digits(month, s.append('-'))
        append2Digits(day, s.append('-'))
        s.toString
      case 3 =>
        append4Digits(year, s)
        append2Digits(month, s.append('-'))
        append2Digits(day, s.append('-'))
        appendTime(secsOfDay, s.append('T'))
        s.append('Z').toString
      case _ =>
        append4Digits(year, s)
        append2Digits(month, s.append('-'))
        append2Digits(day, s.append('-'))
        appendTime(secsOfDay, s.append('T'))
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

  private[this] def appendTime(secsOfDay: Int, s: java.lang.StringBuilder): Unit = {
    val y1 = secsOfDay * 1193047L // Based on James Anhalt's algorithm: https://jk-jeon.github.io/posts/2022/02/jeaiii-algorithm/
    val y2 = (y1 & 0xFFFFFFFFL) * 60
    val y3 = (y2 & 0xFFFFFFFFL) * 60
    append2Digits((y1 >> 32).toInt, s)
    append2Digits((y2 >> 32).toInt, s.append(':'))
    append2Digits((y3 >> 32).toInt, s.append(':'))
  }

  private[this] def appendNano(nano: Int, s: java.lang.StringBuilder): Unit =
    if (nano != 0) {
      val y1 = nano * 1441151881L // Based on James Anhalt's algorithm for 9 digits: https://jk-jeon.github.io/posts/2022/02/jeaiii-algorithm/
      val y2 = (y1 & 0x1FFFFFFFFFFFFFFL) * 100
      s.append('.').append(((y1 >>> 57).toInt + '0').toChar)
      append2Digits((y2 >>> 57).toInt, s)
      if ((y2 & 0x1FFFFF800000000L) != 0) { // check if nano is divisible by 1000000
        val y3 = (y2 & 0x1FFFFFFFFFFFFFFL) * 100
        val y4 = (y3 & 0x1FFFFFFFFFFFFFFL) * 100
        append2Digits((y3 >>> 57).toInt, s)
        val d = digits((y4 >>> 57).toInt)
        s.append((d & 0xFF).toChar)
        if ((y4 & 0x1FF000000000000L) != 0 || d > 0x3039) { // check if nano is divisible by 1000
          append2Digits(((y4 & 0x1FFFFFFFFFFFFFFL) * 100 >>> 57).toInt, s.append((d >> 8).toChar))
        }
      }
    }

  private[this] def append4Digits(x: Int, s: java.lang.StringBuilder): Unit = {
    val q = x * 5243 >> 19 // divide a 4-digit positive int by 100
    append2Digits(q, s)
    append2Digits(x - q * 100, s)
  }

  private[this] def append2Digits(x: Int, s: java.lang.StringBuilder): Unit = {
    val d = digits(x)
    val _ = s.append((d & 0xFF).toChar).append((d >> 8).toChar)
  }

  private[this] def to400YearCycle(day: Long): Int =
    (day / 146097).toInt // 146097 == number of days in a 400 year cycle

  private[this] def toMarchDayOfYear(marchZeroDay: Long, year: Int): Int = {
    val century = (year * 1374389535L >> 37).toInt // divide an int by 100 (the sign correction is not needed)
    (marchZeroDay - year * 365L).toInt - (year >> 2) + century - (century >> 2)
  }
}

object Timestamp {
  private val digits: Array[Short] = Array(
    0x3030, 0x3130, 0x3230, 0x3330, 0x3430, 0x3530, 0x3630, 0x3730, 0x3830, 0x3930,
    0x3031, 0x3131, 0x3231, 0x3331, 0x3431, 0x3531, 0x3631, 0x3731, 0x3831, 0x3931,
    0x3032, 0x3132, 0x3232, 0x3332, 0x3432, 0x3532, 0x3632, 0x3732, 0x3832, 0x3932,
    0x3033, 0x3133, 0x3233, 0x3333, 0x3433, 0x3533, 0x3633, 0x3733, 0x3833, 0x3933,
    0x3034, 0x3134, 0x3234, 0x3334, 0x3434, 0x3534, 0x3634, 0x3734, 0x3834, 0x3934,
    0x3035, 0x3135, 0x3235, 0x3335, 0x3435, 0x3535, 0x3635, 0x3735, 0x3835, 0x3935,
    0x3036, 0x3136, 0x3236, 0x3336, 0x3436, 0x3536, 0x3636, 0x3736, 0x3836, 0x3936,
    0x3037, 0x3137, 0x3237, 0x3337, 0x3437, 0x3537, 0x3637, 0x3737, 0x3837, 0x3937,
    0x3038, 0x3138, 0x3238, 0x3338, 0x3438, 0x3538, 0x3638, 0x3738, 0x3838, 0x3938,
    0x3039, 0x3139, 0x3239, 0x3339, 0x3439, 0x3539, 0x3639, 0x3739, 0x3839, 0x3939
  )
  private val daysOfWeek: Array[String] = Array("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
  private val months: Array[String] =
    Array("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

  def apply(epochSecond: Long, nano: Int): Timestamp = {
    require(epochSecond >= -62167219200L && epochSecond <= 253402300799L, "illegal epochSecond")
    require(nano >= 0 && nano <= 999999999, "illegal nano")
    new Timestamp(epochSecond, nano)
  }

  def apply(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0, second: Int = 0,
            nano: Int = 0): Timestamp = {
    require(year >= 0 && year <= 9999, "illegal year")
    require(month >= 1 && month <= 12, "illegal month")
    require(day >= 1 && (day <= 28 || day <= maxDayForYearMonth(year, month)), "illegal year, month, day combination")
    require(hour >= 0 && hour <= 23, "illegal hour")
    require(minute >= 0 && minute <= 59, "illegal minute")
    require(second >= 0 && second <= 59, "illegal second")
    require(nano >= 0 && nano <= 999999999, "illegal nano")
    new Timestamp(toEpochDay(year, month, day) * 86400 + (hour * 3600 + minute * 60 + second), nano)
  }

  def fromEpochSecond(epochSecond: Long): Timestamp = Timestamp(epochSecond, 0)

  /** JVM platform only method */
  def fromInstant(x: Instant): Timestamp = Timestamp(x.getEpochSecond, x.getNano)

  /** JVM platform only method */
  def fromOffsetDateTime(x: OffsetDateTime): Timestamp = Timestamp(x.toInstant.getEpochSecond, x.getNano)

  def nowUTC(): Timestamp = fromInstant(Instant.now())

  def parse(string: String, format: TimestampFormat): Option[Timestamp] = try {
    new Some(format match {
      case TimestampFormat.DATE_TIME     => parseDateTime(string)
      case TimestampFormat.EPOCH_SECONDS => parseEpochSeconds(string)
      case TimestampFormat.HTTP_DATE     => parseHTTPDate(string)
    })
  } catch {
    case _: Throwable => None
  }

  def showFormat(format: TimestampFormat): String = format match {
    case TimestampFormat.DATE_TIME     => "date-time timestamp (YYYY-MM-DDThh:mm:ss.sssZ)"
    case TimestampFormat.EPOCH_SECONDS => "epoch-second timestamp"
    case TimestampFormat.HTTP_DATE     => "http-date timestamp (EEE, dd MMM yyyy HH:mm:ss.sss z)"
  }

  private[this] def parseDateTime(s: String): Timestamp = {
    val len = s.length
    if (len < 19) error()
    var pos = 0
    val year = {
      val ch0 = s.charAt(pos)
      val ch1 = s.charAt(pos + 1)
      val ch2 = s.charAt(pos + 2)
      val ch3 = s.charAt(pos + 3)
      val ch4 = s.charAt(pos + 4)
      if (ch0 < '0' || ch0 > '9' || ch1 < '0' || ch1 > '9' || ch2 < '0' || ch2 > '9' || ch3 < '0' || ch3 > '9'
        || ch4 != '-') error()
      pos += 5
      ch0 * 1000 + ch1 * 100 + ch2 * 10 + ch3 - 53328 // 53328 == '0' * 1111
    }
    val month = {
      val ch0 = s.charAt(pos)
      val ch1 = s.charAt(pos + 1)
      val ch2 = s.charAt(pos + 2)
      val month = ch0 * 10 + ch1 - 528 // 528 == '0' * 11
      if (ch0 < '0' || ch0 > '1' || ch1 < '0' || ch1 > '9' || month < 1 || month > 12 || ch2 != '-') error()
      pos += 3
      month
    }
    val day = {
      val ch0 = s.charAt(pos)
      val ch1 = s.charAt(pos + 1)
      val ch2 = s.charAt(pos + 2)
      val day = ch0 * 10 + ch1 - 528 // 528 == '0' * 11
      if (ch0 < '0' || ch0 > '3' || ch1 < '0' || ch1 > '9' || day == 0 ||
        (day > 28 && day > maxDayForYearMonth(year, month)) || ch2 != 'T') error()
      pos += 3
      day
    }
    val hour = {
      val ch0 = s.charAt(pos)
      val ch1 = s.charAt(pos + 1)
      val ch2 = s.charAt(pos + 2)
      val hour = ch0 * 10 + ch1 - 528 // 528 == '0' * 11
      if (ch0 < '0' || ch0 > '2' || ch1 < '0' || ch1 > '9' || hour > 23 || ch2 != ':') error()
      pos += 3
      hour
    }
    val minute = {
      val ch0 = s.charAt(pos)
      val ch1 = s.charAt(pos + 1)
      val ch2 = s.charAt(pos + 2)
      if (ch0 < '0' || ch0 > '5' || ch1 < '0' || ch1 > '9' || ch2 != ':') error()
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
        while (pos < len && {
          ch = s.charAt(pos)
          pos += 1
          (ch >= '0' && ch <= '9') && nanoDigitWeight != 0
        }) {
          nano += (ch - '0') * nanoDigitWeight
          nanoDigitWeight = (nanoDigitWeight * 429496730L >> 32).toInt // divide a small positive int by 10
        }
      }
    }
    if (ch != 'Z' || pos != len) error()
    new Timestamp(toEpochDay(year, month, day) * 86400 + (hour * 3600 + minute * 60 + second), nano)
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
    while (pos < len && {
      ch = s.charAt(pos)
      ch >= '0' && ch <= '9'
    }) {
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
        while (pos < len && {
          ch = s.charAt(pos)
          pos += 1
          (ch >= '0' && ch <= '9') && nanoDigitWeight != 0
        }) {
          nano += (ch - '0') * nanoDigitWeight
          nanoDigitWeight = (nanoDigitWeight * 429496730L >> 32).toInt // divide a small positive int by 10
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
      while (i < 7 && {
        val d = daysOfWeek(i)
        d.charAt(0) != ch0 || d.charAt(1) != ch1 || d.charAt(2) != ch2
      }) i += 1
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
      if (ch0 < '0' || ch0 > '3' || ch1 < '0' || ch1 > '9' || day == 0 || day > 31 || ch2 != ' ') error()
      pos += 3
      day
    }
    val month = {
      val ch0 = s.charAt(pos)
      val ch1 = s.charAt(pos + 1)
      val ch2 = s.charAt(pos + 2)
      val ch3 = s.charAt(pos + 3)
      var i = 0
      while (i < 12 && {
        val m = months(i)
        m.charAt(0) != ch0 || m.charAt(1) != ch1 || m.charAt(2) != ch2
      }) i += 1
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
      if (ch0 < '0' || ch0 > '9' || ch1 < '0' || ch1 > '9' || ch2 < '0' || ch2 > '9' || ch3 < '0' || ch3 > '9'
        || ch4 != ' ') error()
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
      if (ch0 < '0' || ch0 > '2' || ch1 < '0' || ch1 > '9' || hour > 23 || ch2 != ':') error()
      pos += 3
      hour
    }
    val minute = {
      val ch0 = s.charAt(pos)
      val ch1 = s.charAt(pos + 1)
      val ch2 = s.charAt(pos + 2)
      if (ch0 < '0' || ch0 > '5' || ch1 < '0' || ch1 > '9' || ch2 != ':') error()
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
        while (pos < len && {
          ch = s.charAt(pos)
          pos += 1
          (ch >= '0' && ch <= '9') && nanoDigitWeight != 0
        }) {
          nano += (ch - '0') * nanoDigitWeight
          nanoDigitWeight = (nanoDigitWeight * 429496730L >> 32).toInt // divide a small positive int by 10
        }
      }
    }
    if (ch != ' ' || pos + 2 >= len || {
      val ch0 = s.charAt(pos)
      val ch1 = s.charAt(pos + 1)
      val ch2 = s.charAt(pos + 2)
      pos += 3
      ch0 != 'G' || ch1 != 'M' || ch2 != 'T'
    }) error()
    new Timestamp(epochDay * 86400 + (hour * 3600 + minute * 60 + second), nano)
  }

  private[this] def toEpochDay(year: Int, month: Int, day: Int): Long =
    year * 365L + ((year + 3 >> 2) - {
      val cp = year * 1374389535L
      if (year < 0) (cp >> 37) - (cp >> 39) // year / 100 - year / 400
      else (cp + 136064563965L >> 37) - (cp + 548381424465L >> 39) // (year + 99) / 100 - (year + 399) / 400
    }.toInt + (month * 1002277 - 988622 >> 15) - // (month * 367 - 362) / 12
      (if (month <= 2) 0
      else if (isLeap(year)) 1
      else 2) + day - 719529) // 719528 == days 0000 to 1970

  private[this] def maxDayForYearMonth(year: Int, month: Int): Int =
    if (month != 2) ((month >> 3) ^ (month & 0x1)) + 30
    else if (isLeap(year)) 29
    else 28

  private[this] def isLeap(year: Int): Boolean = (year & 0x3) == 0 && { // (year % 100 != 0 || year % 400 == 0)
    val cp = year * 1374389535L
    val cc = year >> 31
    ((cp ^ cc) & 0x1FC0000000L) != 0 || (((cp >> 37) - cc) & 0x3) == 0
  }

  private[this] def error(): Throwable = throw new RuntimeException with NoStackTrace
}
