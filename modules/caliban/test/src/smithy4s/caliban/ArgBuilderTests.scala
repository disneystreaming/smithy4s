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
import smithy4s.example.Rec
import smithy4s.Document
import java.time.Instant
import smithy4s.Timestamp
import smithy.api.TimestampFormat
import smithy4s.ByteArray

object ArgBuilderTests extends FunSuite {
  // todo: tests for negative paths

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

  test("list") {
    decodeArgSuccess(
      InputValue.ListValue(
        List(
          Value.StringValue("test"),
          Value.StringValue("test2")
        )
      ),
      List("test", "test2")
    )(Schema.list(Schema.string))
  }

  test("vector") {
    decodeArgSuccess(
      InputValue.ListValue(
        List(
          Value.StringValue("test"),
          Value.StringValue("test2")
        )
      ),
      Vector("test", "test2")
    )(Schema.vector(Schema.string))
  }

  test("indexedSeq") {
    decodeArgSuccess(
      InputValue.ListValue(
        List(
          Value.StringValue("test"),
          Value.StringValue("test2")
        )
      ),
      IndexedSeq("test", "test2")
    )(Schema.indexedSeq(Schema.string))
  }

  test("set") {
    decodeArgSuccess(
      InputValue.ListValue(
        List(
          Value.StringValue("test"),
          Value.StringValue("test")
        )
      ),
      Set("test")
    )(Schema.set(Schema.string))
  }

  test("map") {
    decodeArgSuccess(
      InputValue.ObjectValue(
        Map(
          "test" -> Value.StringValue("test"),
          "test2" -> Value.StringValue("test2")
        )
      ),
      Map("test" -> "test", "test2" -> "test2")
    )(Schema.map(Schema.string, Schema.string))
  }

  test("recursive struct") {
    decodeArgSuccess(
      InputValue.ObjectValue(
        Map(
          "name" -> Value.StringValue("level1"),
          "next" -> InputValue.ObjectValue(
            Map(
              "name" -> Value.StringValue("level2"),
              "next" -> InputValue.ObjectValue(
                Map(
                  "name" -> Value.StringValue("level3")
                )
              )
            )
          )
        )
      ),
      Rec(
        name = "level1",
        next = Some(
          Rec(name = "level2", next = Some(Rec(name = "level3", next = None)))
        )
      )
    )
  }

  test("timestamp (default: epoch second)") {
    decodeArgSuccess(
      Value.IntValue(100L),
      Timestamp.fromEpochSecond(100L)
    )(Schema.timestamp)
  }

  test("timestamp (DATE_TIME)") {
    decodeArgSuccess(
      Value.StringValue(Instant.EPOCH.toString()),
      Timestamp.fromInstant(Instant.EPOCH)
    )(Schema.timestamp.addHints(TimestampFormat.DATE_TIME.widen))
  }

  test("timestamp (HTTP_DATE)") {
    decodeArgSuccess(
      Value.StringValue("Tue, 29 Apr 2014 18:30:38 GMT"),
      Timestamp(
        year = 2014,
        month = 4,
        day = 29,
        hour = 18,
        minute = 30,
        second = 38
      )
    )(Schema.timestamp.addHints(TimestampFormat.HTTP_DATE.widen))
  }

  test("short") {
    decodeArgSuccess(
      Value.IntValue(Short.MaxValue),
      Short.MaxValue
    )(Schema.short)
  }

  test("byte") {
    decodeArgSuccess(
      Value.IntValue(Byte.MaxValue),
      Byte.MaxValue
    )(Schema.byte)
  }

  test("bytes") {
    decodeArgSuccess(
      Value.StringValue(ByteArray(Array(1, 2, 3)).toString),
      ByteArray(Array(1, 2, 3))
    )(Schema.bytes)
  }

  test("document") {
    decodeArgSuccess(
      InputValue.ObjectValue(
        Map(
          "str" -> Value.StringValue("test"),
          "int" -> Value.IntValue(42),
          "long" -> Value.IntValue.LongNumber(42L),
          "float" -> Value.FloatValue(42.0f),
          "double" -> Value.FloatValue.DoubleNumber(42.0d),
          "bigint" -> Value.IntValue.BigIntNumber(BigInt(10)),
          "bigdecimal" -> Value.FloatValue.BigDecimalNumber(BigDecimal(10)),
          "arr" -> InputValue.ListValue(
            List(
              Value.StringValue("string in list"),
              InputValue.ObjectValue(
                Map("k1" -> Value.StringValue("in array object"))
              )
            )
          ),
          "obj" -> InputValue.ObjectValue(
            Map(
              "k2" -> Value.StringValue("in object")
            )
          )
        )
      ),
      Document.obj(
        "str" -> Document.fromString("test"),
        "int" -> Document.fromInt(42),
        "long" -> Document.fromLong(42L),
        "float" -> Document.fromDouble(42.0d),
        "double" -> Document.fromDouble(42.0d),
        "bigint" -> Document.fromBigDecimal(BigDecimal(10)),
        "bigdecimal" -> Document.fromBigDecimal(BigDecimal(10)),
        "arr" -> Document.array(
          Document.fromString("string in list"),
          Document.obj(
            "k1" -> Document.fromString("in array object")
          )
        ),
        "obj" -> Document.obj("k2" -> Document.fromString("in object"))
      )
    )(Schema.document)
  }
}
