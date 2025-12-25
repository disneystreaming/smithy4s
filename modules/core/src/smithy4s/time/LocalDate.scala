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

case class LocalDate private (epochDay: Long) extends LocalDatePlatform {

  def isAfter(other: LocalDate): Boolean = {
    val diff = epochDay - other.epochDay
    diff > 0
  }

  override def toString: String = {
    val s = new java.lang.StringBuilder(32)
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
    s.toString
  }
}

object LocalDate extends LocalDateCompanionPlatform {

  val epoch = LocalDate(0)

  def apply(epochDay: Long): LocalDate = new LocalDate(epochDay)

  def apply(
      year: Int,
      month: Int,
      day: Int
  ): LocalDate = {
    require(year >= 0 && year <= 9999, "illegal year")
    require(month >= 1 && month <= 12, "illegal month")
    require(
      day >= 1 && (day <= 28 || day <= TimeUtil
        .maxDayForYearMonth(year, month)),
      "illegal year, month, day combination"
    )
    new LocalDate(
      TimeUtil.toEpochDay(
        year,
        month,
        day
      )
    )
  }

  def parse(s: String): Option[LocalDate] = try {
    Some(parseUnsafe(s))
  } catch {
    case NonFatal(_) => None
  }

  def parseUnsafe(s: String): LocalDate = {
    val len = s.length
    if (len < 10) error()
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
      val day = ch0 * 10 + ch1 - 528 // 528 == '0' * 11
      if (
        ch0 < '0' || ch0 > '3' || ch1 < '0' || ch1 > '9' || day == 0 ||
        (day > 28 && day > TimeUtil.maxDayForYearMonth(year, month))
      ) error()
      pos += 2
      day
    }
    if (pos != len) error()
    LocalDate(year, month, day)
  }

  private[this] def error(): Throwable = throw new RuntimeException
    with NoStackTrace
}
