/*
 *  Copyright 2021-2023 Disney Streaming
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

import cats.kernel.Eq
import smithy.api._
import alloy.Discriminated

class HintMaskSpec() extends munit.FunSuite {

  private implicit val hintsEq: Eq[Hints] = (x: Hints, y: Hints) =>
    x.toMap == y.toMap

  test("Hints are masked") {
    val hints = Hints(Discriminated("type"), Required(), HttpError(404))
    val mask = HintMask(Discriminated, Required)
    val result = mask(hints)
    val expected = Hints(Discriminated("type"), Required())
    expect.eql(expected, result)
  }

  test("empty mask masks all hints") {
    val hints = Hints(Discriminated("type"), Required(), HttpError(404))
    val mask = HintMask.empty
    val result = mask(hints)
    expect.eql(Hints(), result)
  }
}
