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

final class SmithyResourcesSpec extends FunSuite {
  test("produce smithy4s metadata files") {
    val resDir = os.temp.dir()

    val paths = SmithyResources.produce(
      resDir,
      List(
        os.Path(
          "/Users/David.Francoeur/workspace/dev/smithy4s/sampleSpecs/greet.smithy"
        )
      ),
      List("smithy4s.example.greet")
    )
    assertEquals(
      paths.map(_.toPath.relativeTo(resDir)),
      List(
        os.RelPath("META-INF/smithy/greet.smithy"),
        os.RelPath("META-INF/smithy/manifest"),
        os.RelPath("META-INF/smithy/smithy4s.tracking.smithy")
      )
    )
  }
}
