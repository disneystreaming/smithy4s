/*
 *  Copyright 2021-2022 Disney Streaming
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

package smithy4s.dynamic

import weaver._

object RecursionSpec extends SimpleIOSuite {

  test(
    "Compilation does not recurse infinitely in the case of recursive unions".only
  ) {
    val modelString =
      """|namespace foo
         |
         |union RecursiveUnion {
         |  empty: Unit,
         |  nonEmpty: RecursiveUnion
         |}
         |""".stripMargin

    Utils.compile(modelString).as(success)
  }

  test(
    "Compilation does not recurse infinitely in the case of recursive structures".only
  ) {
    val modelString =
      """|namespace foo
         |
         |structure RecursiveStructure1 {
         |  rec2: RecursiveStructure2
         |}
         |
         |structure RecursiveStructure2 {
         |  rec1: RecursiveStructure1
         |} 
         |""".stripMargin

    Utils.compile(modelString).as(success)
  }

}
