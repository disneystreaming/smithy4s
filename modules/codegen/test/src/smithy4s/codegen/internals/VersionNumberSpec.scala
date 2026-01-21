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

package smithy4s.codegen.internals

import cats.syntax.all._
import munit._

final class VersionNumberSpec extends FunSuite {
  test("VersionNumber ordering") {
    assert(VersionNumber.parse("1.0") < VersionNumber.parse("1.0.1"))
    assert(VersionNumber.parse("1.0.1") < VersionNumber.parse("1.1"))
    assert(VersionNumber.parse("1.1.2") < VersionNumber.parse("1.1.3"))
    assert(VersionNumber.parse("1.1.9") < VersionNumber.parse("1.1.11"))
  }
}
