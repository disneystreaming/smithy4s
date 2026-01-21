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

package smithy4s.example

import munit.FunSuite

class OpenEnumTestsSpec extends FunSuite {

  test("OpenEnumTest uses fromStringOrUnknown for known and unknown values") {
    assertEquals(OpenEnumTest.fromStringOrUnknown("ONE"), OpenEnumTest.ONE)
    assertEquals(
      OpenEnumTest.fromStringOrUnknown("UNKNOWN_VALUE"),
      OpenEnumTest.$Unknown("UNKNOWN_VALUE")
    )
  }

  test("OpenIntEnumTest uses fromIntOrUnknown for known and unknown values") {
    assertEquals(OpenIntEnumTest.fromIntOrUnknown(1), OpenIntEnumTest.ONE)
    assertEquals(
      OpenIntEnumTest.fromIntOrUnknown(999),
      OpenIntEnumTest.$Unknown(999)
    )
  }

}
