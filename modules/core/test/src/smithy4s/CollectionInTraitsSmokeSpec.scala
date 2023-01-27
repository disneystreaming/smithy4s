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

import cats.syntax.all._
import munit._

class CollectionInTraitsSmokeSpec() extends FunSuite {

  test("Traits with collection members do not refer to them using newtypes") {
    val (someList, someSet, someMap) = smithy4s.example.SomeInt.hints
      .get(smithy4s.example.SomeCollections)
      .foldMap(x => (x.someList, x.someSet, x.someMap))

    expect.eql(someList, List("a"))
    expect.eql(someSet, Set("b"))
    expect.eql(someMap, Map("a" -> "b"))
  }

}
