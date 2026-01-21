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

import java.time.{OffsetDateTime => JOffsetDateTime}

private[time] trait OffsetDateTimeCompanionPlatform {

  /** JVM platform only method */
  def fromJava(x: JOffsetDateTime): OffsetDateTime = {
    OffsetDateTime(
      x.getYear(),
      x.getMonthValue(),
      x.getDayOfMonth(),
      x.getHour(),
      x.getMinute(),
      x.getSecond(),
      x.getNano(),
      ZoneOffset(x.getOffset().getTotalSeconds)
    )
  }

  /** JVM platform only method */
  def toJava(x: OffsetDateTime): JOffsetDateTime = {
    val instant = java.time.Instant
      .ofEpochSecond(x.timestamp.epochSecond, x.timestamp.nano.toLong)
    val offset = java.time.ZoneOffset.ofTotalSeconds(x.offset.seconds)
    java.time.OffsetDateTime.ofInstant(instant, offset)
  }

  def now(): OffsetDateTime = fromJava(JOffsetDateTime.now())
}
