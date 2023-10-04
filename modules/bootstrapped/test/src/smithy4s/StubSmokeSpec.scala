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

import munit._
class DefaultServiceSmokeSpec() extends FunSuite {

  test("Default stubs do compile") {
    object stub extends smithy4s.example.Weather.Default[Option](None)
    val expected: Option[smithy4s.example.GetCurrentTimeOutput] = None
    // calling _.time to verify type inference
    val result = stub.getCurrentTime().map(_.time)
    expect.same(result, expected)
  }

}
