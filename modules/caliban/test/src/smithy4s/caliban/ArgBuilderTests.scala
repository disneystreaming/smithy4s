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

package smithy4s.caliban

import weaver._
import smithy4s.Schema
import caliban.Value
import smithy4s.Bijection
import smithy.api.Length
import caliban.InputValue
import smithy4s.example.CityCoordinates
import smithy4s.example.MovieTheater
import smithy4s.example.Foo
import smithy4s.example.Ingredient
import smithy4s.example.EnumResult

object ArgBuilderTests extends FunSuite {

  private def decodeArgSuccess[A](
      value: InputValue,
      expected: A
  )(implicit schema: Schema[A]) =
    assert.same(
      schema
        .compile(ArgBuilderVisitor)
        .build(value),
      Right(expected)
    )
  test("string") {
    decodeArgSuccess(
      Value.StringValue("test"),
      "test"
    )(
      Schema.string
    )
  }

  test("bijection") {
    decodeArgSuccess(
      Value.StringValue("test"),
      "TEST"
    )(
      Schema.string.biject(
        Bijection[String, String](_.toUpperCase, identity(_))
      )
    )
  }

  test("refinement") {
    decodeArgSuccess(
      Value.StringValue("test"),
      "test"
    )(
      Schema.string.refined(Length(min = Some(1)))
    )
  }

  test("structure schema - all fields required") {
    decodeArgSuccess(
      InputValue.ObjectValue(
        Map(
          "latitude" -> Value.FloatValue(42.0f),
          "longitude" -> Value.FloatValue(21.37f)
        )
      ),
      CityCoordinates(latitude = 42.0f, longitude = 21.37f)
    )
  }

  test("structure schema - missing optional field") {
    decodeArgSuccess(
      InputValue.ObjectValue(
        Map.empty
      ),
      MovieTheater(name = None)
    )
  }

  test("structure schema - null optional field") {
    decodeArgSuccess(
      InputValue.ObjectValue(
        Map(
          "name" -> Value.NullValue
        )
      ),
      MovieTheater(name = None)
    )
  }

  test("structure schema - present optional field") {
    decodeArgSuccess(
      InputValue.ObjectValue(
        Map(
          "name" -> Value.StringValue("cinema")
        )
      ),
      MovieTheater(name = Some("cinema"))
    )
  }

  test("union schema") {
    decodeArgSuccess(
      InputValue.ObjectValue(
        Map(
          "str" -> Value.StringValue("test")
        )
      ),
      Foo.StrCase("test").widen
    )
  }

  test("enum schema") {
    decodeArgSuccess(
      Value.StringValue("Tomato"),
      Ingredient.Tomato.widen
    )
  }

  test("int enum schema") {
    decodeArgSuccess(
      Value.IntValue(2),
      EnumResult.SECOND.widen
    )
  }
}
