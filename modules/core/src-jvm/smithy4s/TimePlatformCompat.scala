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

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset

private[smithy4s] trait TimePlatformCompat extends TimestampCompanion {
  def apply(
      year: Int,
      month: Int,
      day: Int,
      hour: Int,
      minute: Int,
      second: Int,
      nanoseconds: Int
  ): Timestamp =
    new TimestampImpl(
      OffsetDateTime
        .of(
          year,
          month,
          day,
          hour,
          minute,
          second,
          nanoseconds,
          ZoneOffset.UTC
        )
    )

  def nowUTC(): Timestamp = {
    val dateTime = OffsetDateTime.now(ZoneOffset.UTC)
    fromOffsetDateTime(dateTime.withNano(0))
  }

  def fromOffsetDateTime(dateTime: OffsetDateTime): Timestamp = {
    new TimestampImpl(dateTime)
  }

  def fromInstant(instant: Instant): Timestamp = {
    new TimestampImpl(OffsetDateTime.ofInstant(instant, ZoneOffset.UTC))
  }

  def fromLocalDate(date: LocalDate): Timestamp = {
    new TimestampImpl(
      OffsetDateTime.of(date, LocalTime.MIDNIGHT, ZoneOffset.UTC)
    )
  }

  def fromEpochSecond(epochSecond: Long): Timestamp =
    new TimestampImpl(
      OffsetDateTime.ofInstant(
        Instant.ofEpochSecond(epochSecond),
        ZoneId.of("UTC")
      )
    )

  protected def parseHttpDate(string: String): Option[Timestamp] = try {
    Some {
      fromOffsetDateTime(
        OffsetDateTime.ofInstant(
          Instant.from(TimestampImpl.imfDateFormatter.parse(string)),
          ZoneOffset.UTC
        )
      )
    }
  } catch { case _: Throwable => None }

  protected def parseDateTime(string: String): Option[Timestamp] =
    try {
      Some {
        fromOffsetDateTime(
          OffsetDateTime.ofInstant(Instant.parse(string), ZoneOffset.UTC)
        )
      }
    } catch { case _: Throwable => None }

}
