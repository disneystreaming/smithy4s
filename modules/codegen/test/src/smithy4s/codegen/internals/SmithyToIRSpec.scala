/*
 *  Copyright 2021-2024 Disney Streaming
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

package smithy4s.codegen.internals

import munit._

final class SmithyToIRSpec extends FunSuite {

  test("prettifyName: sdkId takes precedence") {
    assertEquals(
      SmithyToIR.prettifyName(Some("Example"), "unused"),
      "Example"
    )
  }
  test("prettifyName: shapeName is used as a fallback") {
    assertEquals(
      SmithyToIR.prettifyName(None, "Example"),
      "Example"
    )
  }

  test("prettifyName removes whitespace in sdkId") {
    assertEquals(
      SmithyToIR.prettifyName(Some("QuickDB \t\nStreams"), "unused"),
      "QuickDBStreams"
    )
  }

  // Not a feature, just verifying the name is unaffected
  test("prettifyName ignores whitespace in shape name") {
    assertEquals(
      SmithyToIR.prettifyName(None, "This Has Spaces"),
      "This Has Spaces"
    )
  }
}
