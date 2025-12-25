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

import scalajs.js.Date

private[time] trait OffsetDateTimeCompanionPlatform {

  def fromDate(x: Date): OffsetDateTime = {
    OffsetDateTime(
      x.getFullYear().toInt,
      x.getMonth().toInt + 1,
      x.getDate().toInt,
      x.getHours().toInt,
      x.getMinutes().toInt,
      x.getSeconds().toInt,
      x.getMilliseconds().toInt * 100000,
      // getTimezoneOffset returns the offset in minutes so need to adjust it into seconds
      ZoneOffset(x.getTimezoneOffset().toInt * 60)
    )
  }

  def now(): OffsetDateTime = fromDate(new Date())

}
