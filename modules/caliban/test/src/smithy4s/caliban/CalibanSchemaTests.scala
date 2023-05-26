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
import CalibanTestUtils._
import smithy4s.example.CityCoordinates
import io.circe.syntax._
import io.circe.Json
import smithy4s.example.MovieTheater
import smithy4s.example.Foo
import smithy4s.Schema
import smithy.api.Length
import smithy4s.Bijection
import smithy4s.example.Ingredient
import smithy4s.example.EnumResult

object CalibanSchemaTests extends SimpleIOSuite {
  test("structure schema - all fields required") {
    testQueryResultWithSchema(
      CityCoordinates(latitude = 42.0f, longitude = 21.37f),
      """query {
        |  latitude
        |  longitude
        |}""".stripMargin
    ).map(
      assert.eql(
        _,
        Json.obj(
          "latitude" := 42.0f,
          "longitude" := 21.37f
        )
      )
    )
  }

  test("structure schema - missing optional field") {
    testQueryResultWithSchema(
      MovieTheater(name = None),
      """query {
        |  name
        |}""".stripMargin
    )
      .map(assert.eql(_, Json.obj("name" := Json.Null)))
  }

  test("structure schema - present optional field") {
    testQueryResultWithSchema(
      MovieTheater(name = Some("cinema")),
      """query {
        |  name
        |}""".stripMargin
    )
      .map(assert.eql(_, Json.obj("name" := "cinema")))
  }

  test("union schema") {
    testQueryResultWithSchema(
      Foo.StrCase("myString"): Foo,
      """query {
        |  ... on FoostrCase {
        |    str
        |  }
        |}""".stripMargin
    )
      .map(assert.eql(_, Json.obj("str" := "myString")))
  }

  test("list schema") {

    testQueryResultWithSchema(
      List("a", "b", "c"),
      "query { items }"
    )(Schema.list(Schema.string).nested("items"))
      .map(assert.eql(_, Json.obj("items" := List("a", "b", "c"))))
  }

  test("indexedSeq schema") {
    testQueryResultWithSchema(
      IndexedSeq("a", "b", "c"),
      "query { items }"
    )(Schema.indexedSeq(Schema.string).nested("items"))
      .map(assert.eql(_, Json.obj("items" := List("a", "b", "c"))))
  }

  test("vector schema") {
    testQueryResultWithSchema(
      Vector("a", "b", "c"),
      "query { items }"
    )(Schema.vector(Schema.string).nested("items"))
      .map(assert.eql(_, Json.obj("items" := List("a", "b", "c"))))
  }

  test("set schema") {
    testQueryResultWithSchema(
      Set("a", "b", "c"),
      "query { items }"
    )(Schema.set(Schema.string).nested("items"))
      .map(assert.eql(_, Json.obj("items" := List("a", "b", "c"))))
  }

  test("refinement schema") {

    testQueryResultWithSchema(
      "test",
      "query { item }"
    )(
      Schema.string
        .refined(Length(min = Some(1)))
        .nested("item")
    ).map(assert.eql(_, Json.obj("item" := "test")))
  }

  test("bijection schema") {
    testQueryResultWithSchema(
      "test",
      "query { item }"
    )(
      Schema.string
        .biject(Bijection[String, String](identity, _.toUpperCase()))
        .nested("item")
    )
      .map(assert.eql(_, Json.obj("item" := "TEST")))
  }

  test("enum schema") {
    testQueryResultWithSchema(
      Ingredient.Tomato.widen,
      """query { item }""".stripMargin
    )(Ingredient.schema.nested("item"))
      .map(assert.eql(_, Json.obj("item" := "Tomato")))
  }

  test("int enum schema") {
    testQueryResultWithSchema(
      EnumResult.SECOND.widen,
      """query { item }""".stripMargin
    )(EnumResult.schema.nested("item"))
      .map(assert.eql(_, Json.obj("item" := 2)))
  }

  test("map schema") {
    testQueryResultWithSchema(
      Map("a" -> "b", "c" -> "d"),
      """query {
        | items {
        |   key
        |   value
        |  }
        |}""".stripMargin
    )(Schema.map(Schema.string, Schema.string).nested("items"))
      .map(
        assert.eql(
          _,
          Json.obj(
            "items" := Json.arr(
              Json.obj(
                "key" := "a",
                "value" := "b"
              ),
              Json.obj(
                "key" := "c",
                "value" := "d"
              )
            )
          )
        )
      )
  }
}
