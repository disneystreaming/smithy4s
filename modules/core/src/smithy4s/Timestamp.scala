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
import smithy.api.TimestampFormat.DATE_TIME
import smithy.api.TimestampFormat.EPOCH_SECONDS
import smithy.api.TimestampFormat.HTTP_DATE
import smithy4s.Timestamp.sbExt

/**
  * Platform-agnostic UTC timestamp representation.
  *
  * The [[schematic.TimePlatformCompat]] trait provides a "nowUTC" method to get the current time.
  */
abstract class Timestamp private[smithy4s] () extends TimestampPlatformMethods {
  def year: Int
  def month: Int // 1 to 12
  def day: Int
  def hour: Int
  def minute: Int
  def second: Int
  def nanoseconds: Int
  def epochSecond: Long

  override def toString(): String = format(TimestampFormat.DATE_TIME)

  def isAfter(other: Timestamp): Boolean = {
    year.compareTo(other.year) |
      month.compareTo(other.month) |
      day.compareTo(other.day) |
      hour.compare(other.hour) |
      minute.compare(other.minute) |
      second.compare(other.second) |
      nanoseconds.compare(other.nanoseconds)
  } > 0

  def formatted: String = {
    new StringBuilder()
      .append(year)
      .append('-')
      .appendTime(month)
      .append('-')
      .appendTime(day)
      .append('T')
      .appendTime(hour)
      .append(':')
      .appendTime(minute)
      .append(':')
      .appendTime(second)
      .append('Z')
      .result()
  }

  def conciseDateTime: String =
    new StringBuilder()
      .append(year)
      .appendTime(month)
      .appendTime(day)
      .append('T')
      .appendTime(hour)
      .appendTime(minute)
      .appendTime(second)
      .append('Z')
      .result()

  def conciseDate: String =
    new StringBuilder()
      .append(year)
      .appendTime(month)
      .appendTime(day)
      .result()

  def format(format: smithy.api.TimestampFormat): String

}

/**
  * The [[smithy4s.TimePlatformCompat]] contains all the platform-specific
  * code that has to do with retrieving time from the system, and is provided
  * for the hree platforms (jvm/js/native.)
  */
object Timestamp extends TimePlatformCompat {

  private[smithy4s] implicit class sbExt(val sb: StringBuilder) extends AnyVal {
    def appendTime(i: Int): StringBuilder =
      (if (i < 10) sb.append(0) else sb).append(i)
  }

  def showFormat(format: smithy.api.TimestampFormat): String = format match {
    case DATE_TIME     => "date-time timestamp (YYYY-MM-DDThh:mm:ss.sssZ)"
    case EPOCH_SECONDS => "epoch-second timestamp"
    case HTTP_DATE     => "http-date timestamp (EEE, dd MMM yyyy HH:mm:ss z)"
  }

}

/**
  * Trait that is meant to be implemented in platform-specific ways, abstracing over logic
  * that has to do with retrieving time from the system.
  */
trait TimestampCompanion {
  def apply(
      year: Int,
      month: Int,
      day: Int,
      hour: Int,
      minute: Int,
      second: Int,
      nanoseconds: Int = 0
  ): Timestamp

  def nowUTC(): Timestamp

  def fromEpochSecond(epochSecond: Long): Timestamp

  def parse(
      string: String,
      format: smithy.api.TimestampFormat
  ): Option[Timestamp] =
    format match {
      case DATE_TIME => parseDateTime(string)
      case EPOCH_SECONDS =>
        try {
          val len = string.length
          var pos = 0
          var ch = string.charAt(pos)
          pos += 1
          val isNeg = ch == '-'
          if (isNeg) {
            ch = string.charAt(pos)
            pos += 1
          }
          if (ch < '0' || ch > '9') throw new Error
          var epochSecond: Long = (ch - '0').toLong
          while (
            pos < len && {
              ch = string.charAt(pos)
              ch >= '0' && ch <= '9'
            }
          ) {
            epochSecond = epochSecond * 10 + (ch - '0')
            if (epochSecond > 31556889864403200L) throw new Error
            pos += 1
          }
          if (isNeg) epochSecond = -epochSecond
          var nano = 0
          if (ch == '.') {
            pos += 1
            var nanoDigitWeight = 100000000
            while (
              pos < len && {
                ch = string.charAt(pos)
                pos += 1
                ch >= '0' && ch <= '9' && nanoDigitWeight != 0
              }
            ) {
              nano += (ch - '0') * nanoDigitWeight
              nanoDigitWeight =
                (nanoDigitWeight * 3435973837L >> 35).toInt // divide a positive int by 10
            }
          }
          if (pos != len) throw new Error
          val epochDay = // FIXME: Use (Math.multiplyHigh(if (epochSecond >= 0) epochSecond else epochSecond - 86399, 1749024623285053783L) >> 13) - (epochSecond >> 63) after dropping JDK 8 support
            (if (epochSecond >= 0) epochSecond
             else epochSecond - 86399) / 86400 // 86400 == seconds per day
          val secsOfDay = (epochSecond - epochDay * 86400).toInt
          var marchZeroDay =
            epochDay + 719468 // 719468 == 719528 - 60 == days 0000 to 1970 - days 1st Jan to 1st Mar
          var adjustYear = 0
          if (marchZeroDay < 0) { // adjust negative years to positive for calculation
            val marchZeroDayP1 = marchZeroDay + 1
            val adjust400YearCycles =
              (((marchZeroDayP1 * 7525902) >> 40) + (~marchZeroDayP1 >> 63)).toInt // ((marchZeroDay + 1) / 146097).toInt - 1 (146097 == number of days in a 400 year cycle)
            adjustYear = adjust400YearCycles * 400
            marchZeroDay -= adjust400YearCycles * 146097L // 146097 == number of days in a 400 year cycle
          }
          var year = // FIXME: Use { val pa = marchZeroDay * 400 + 591; ((Math.multiplyHigh(pa, 4137408090565272301L) >> 15) + (pa >> 63)).toInt } after dropping JDK 8 support
            ((marchZeroDay * 400 + 591) / 146097).toInt
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
          val hour =
            secsOfDay * 37283 >>> 27 // divide a small positive int by 3600
          val secsOfHour = secsOfDay - hour * 3600
          val minute =
            secsOfHour * 17477 >> 20 // divide a small positive int by 60
          val second = secsOfHour - minute * 60
          Some(apply(year, month, day, hour, minute, second, nano))
        } catch { case _: Throwable => None }
      case HTTP_DATE =>
        parseHttpDate(string)
    }

  protected def parseHttpDate(s: String): Option[Timestamp]
  protected def parseDateTime(s: String): Option[Timestamp]

  private def toMarchDayOfYear(marchZeroDay: Long, year: Int): Int = {
    val century = year / 100
    (marchZeroDay - year * 365L).toInt - (year >> 2) + century - (century >> 2)
  }

  case class Error private[smithy4s] ()
      extends RuntimeException("", null, true, false)
}
