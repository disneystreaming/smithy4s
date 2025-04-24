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

package smithy4s.dynamic.internals

class IsRecursiveSpec() extends munit.FunSuite {

  test("detect recursive vertices - example 1") {
    val map = Map[Int, Set[Int]](
      1 -> Set(2),
      2 -> Set(3, 4, 5),
      3 -> Set(4),
      5 -> Set(6, 7),
      6 -> Set(2),
      7 -> Set(2)
    )
    val result = recursiveVertices(map)
    assertEquals(result, Set(2, 5, 6, 7))
  }

  test("detect recursive vertices - example 2") {
    val map = Map(
      2 -> Set(1, 3),
      1 -> Set(3),
      3 -> Set(4, 5),
      4 -> Set(2),
      5 -> Set(2)
    )
    val result = recursiveVertices(map)
    assertEquals(result, Set(1, 2, 3, 4, 5))
  }

  test("detect recursive vertices - example 3") {
    val map = Map(
      2 -> Set(1),
      1 -> Set(3),
      3 -> Set(1, 2, 3, 4, 5),
      4 -> Set(3, 5),
      5 -> Set(3, 4)
    )
    val result = recursiveVertices(map)
    assertEquals(result, Set(1, 2, 3, 4, 5))
  }

}
