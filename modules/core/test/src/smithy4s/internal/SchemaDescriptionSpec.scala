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

package smithy4s
package internals

import smithy4s.example._
import munit._

class SchemaDescriptionSpec() extends FunSuite {
  def simple[A](s: Schema[A]): String = s.compile(SchemaDescription)
  def detailed[A](s: Schema[A]): String =
    s.compile(SchemaDescriptionDetailed)(Set.empty)._2

  test("simple") {
    assertEquals(simple(SomeInt.schema), "Int")
    assertEquals(simple(SomeSet.schema), "Set")
    assertEquals(simple(SomeList.schema), "List")

    assertEquals(simple(BlobBody.schema), "Structure")
    assertEquals(simple(IntList.schema), "Structure")
  }

  test("detailed") {
    assertEquals(detailed(SomeInt.schema), "Bijection { Int }")
    assertEquals(detailed(SomeSet.schema), "Bijection { Set[String] }")
    assertEquals(detailed(SomeList.schema), "Bijection { List[String] }")

    assertEquals(detailed(BlobBody.schema), "Structure { blob: Bytes }") //
    assertEquals(detailed(IntList.schema), "Structure") //
  }
}
