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

import scala.concurrent.duration.Duration
import scala.concurrent.duration.DurationLong

object DurationOps {
  private val nanoScale = 1000000000L

  def fromBigDecimal(x: BigDecimal): Duration = {
    val seconds = x.setScale(0, BigDecimal.RoundingMode.FLOOR).toLong
    val nanos = ((x - seconds) * nanoScale).toLong

    seconds.seconds + nanos.nanos
  }

  implicit class DurationSyntax(dur: Duration) {
    def toBigDecimal: BigDecimal = {
      val seconds = BigDecimal(dur.toSeconds)
      val nanos = dur.toNanos - (dur.toSeconds * nanoScale)

      if (nanos == 0) seconds
      else seconds + java.math.BigDecimal.valueOf(nanos, 9).stripTrailingZeros
    }
  }
}
