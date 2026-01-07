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

import scalajs.js.Date

private[time] trait LocalDateCompanionPlatform {

  /** JS platform only method */
  def fromDate(x: Date): LocalDate = {
    val year = x.getFullYear().toInt
    val month = x.getMonth().toInt + 1
    val day = x.getDate().toInt

    LocalDate(year, month, day)
  }

  def now(): LocalDate = fromDate(new Date())
}
