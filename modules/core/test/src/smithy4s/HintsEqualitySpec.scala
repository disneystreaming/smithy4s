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

package smithy4s

object HintsEqualitySpec extends weaver.FunSuite {

  test("static equals static") {
    val one: Hint = alloy.DataExample.string("test")
    val two: Hint = alloy.DataExample.string("test")
    expect(one == two) && expect(two == one)
  }

  test("dynamic equals dynamic") {
    val one: Hint = Hints.Binding.DynamicBinding(
      ShapeId(namespace = "alloy", name = "DataExample"),
      Document.DObject(Map("string" -> Document.fromString("test")))
    )
    val two: Hint = Hints.Binding.DynamicBinding(
      ShapeId(namespace = "alloy", name = "DataExample"),
      Document.DObject(Map("string" -> Document.fromString("test")))
    )
    expect(one == two) && expect(two == one)
  }

  test("static equals dynamic") {
    val one: Hint = alloy.DataExample.string("test")
    val two: Hint = Hints.Binding.DynamicBinding(
      ShapeId(namespace = "alloy", name = "DataExample"),
      Document.DObject(Map("string" -> Document.fromString("test")))
    )
    expect(one == two) && expect(two == one)
  }

  test("static NOT equals dynamic") {
    val one: Hint = alloy.DataExample.string("test")
    val two: Hint = Hints.Binding.DynamicBinding(
      ShapeId(namespace = "alloy", name = "DataExample"),
      Document.DObject(Map("string" -> Document.fromString("test2")))
    )
    expect(one != two) && expect(two != one)
  }

  test("static equals static toDynamicBinding") {
    val one: Hint = alloy.DataExample.string("test")
    val two: Hint = Hints.Binding
      .StaticBinding(
        alloy.DataExample,
        alloy.DataExample.string("test")
      )
      .toDynamicBinding
    expect(one == two) && expect(two == one)
  }
}
