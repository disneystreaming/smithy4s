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

package smithy4s.codegen.internals

class NamespacePatternSpec extends munit.FunSuite {

  test("NamespacePattern.matches - positive examples") {
    List(
      "a.b" -> "a.b",
      "a.b.*" -> "a.b.c",
      "a.b.*" -> "a.b.c.d",
      "a.b*" -> "a.b",
      "a.b.*" -> "a.b.C",
      "a.b*" -> "a.bc",
      "a.b*" -> "a.bC",
      "a.b*" -> "a.b.c",
      "a.b*" -> "a.b.C"
    ).foreach { case (pattern, namespace) =>
      assert(
        NamespacePattern.fromString(pattern).matches(namespace),
        s"Pattern '$pattern' expected to match a namespace '$namespace'"
      )
    }
  }

  test("NamespacePattern.matches - negative examples") {
    List(
      "a.b" -> "a.c",
      "a.b" -> "a.B",
      "a.b" -> "acb",
      "a.b.*" -> "a.b",
      "a.b.*" -> "a.B",
      "a.b.*" -> "b.a",
      "a.b.*" -> "a.bb.c",
      "a.b.*" -> "acb.d",
      "a.b*" -> "acb",
      "a.b*" -> "b.a.c",
      "a.b.c-d" -> "a.b.c-d"
    ).foreach { case (pattern, namespace) =>
      assert(
        !NamespacePattern.fromString(pattern).matches(namespace),
        s"Pattern '$pattern' not expected to match a namespace '$namespace'"
      )
    }
  }
}
