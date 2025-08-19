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

import scala.concurrent.duration._
import smithy4s.time.DurationOps._
import smithy4s.expect
import munit._

class DurationOpsSpec() extends FunSuite {

  test("BigDecimal Conversion") {
    val duration = 1.day + 12.hours + 30.minutes + 37.seconds + 500.nanos
    val bigDecimal = BigDecimal(131437.0000005)

    expect.same(duration.toBigDecimal, bigDecimal)
    expect.same(DurationOps.fromBigDecimal(bigDecimal), duration)
  }

}
