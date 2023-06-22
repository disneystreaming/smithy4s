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
package http.internals

import smithy.api.Http
import smithy.api.NonEmptyString
import smithy4s.Schema
import smithy4s.Timestamp
import smithy4s.example.DummyServiceOperation.DummyPath
import smithy4s.example.PathParams
import smithy4s.http.HttpEndpoint
import smithy4s.http.PathSegment

class PathSpec() extends munit.FunSuite {
  import smithy4s.schema.Schema._
  object util {

    def encodePathAs[A](schema: Schema[A]): Option[PathEncode[A]] = {
      val schemaA = schema
        .addHints(
          Http(
            method = NonEmptyString("GET"),
            uri = NonEmptyString("/{label}"),
            code = 200
          )
        )
      SchemaVisitorPathEncoder(schemaA)
    }

    val simpleString = encodePathAs(string)
  }

  test("Parse path pattern into path segments") {
    val result = pathSegments("/{head}/foo/{tail+}")
    expect(
      result == Option(
        Vector(
          PathSegment.label("head"),
          PathSegment.static("foo"),
          PathSegment.greedy("tail")
        )
      )
    )
  }
  test("Parse path pattern from path that has query param into path segments") {
    val result = pathSegments("/{head}/foo/{tail+}?hello=world&hi")
    expect(
      result == Option(
        Vector(
          PathSegment.label("head"),
          PathSegment.static("foo"),
          PathSegment.greedy("tail")
        )
      )
    )
  }
  test("parse static query params from DummyPath") {
    val httpEndpoint = HttpEndpoint
      .cast(
        DummyPath
      )
      .toOption
      .get

    val sqp = httpEndpoint.staticQueryParams
    val path = httpEndpoint.path

    val expectedQueryMap = Map("value" -> Seq("foo"), "baz" -> Seq("bar"))
    expect(sqp == expectedQueryMap)
    expect(
      path ==
        List(
          PathSegment.static("dummy-path"),
          PathSegment.label("str"),
          PathSegment.label("int"),
          PathSegment.label("ts1"),
          PathSegment.label("ts2"),
          PathSegment.label("ts3"),
          PathSegment.label("ts4"),
          PathSegment.label("b"),
          PathSegment.label("ie")
        )
    )
  }

  test("Write PathParams for DummyPath") {
    val result = HttpEndpoint
      .cast(
        DummyPath
      )
      .toTry
      .get
      .path(
        PathParams(
          "example with spaces, %, / and \\",
          10,
          Timestamp(0L, 0),
          Timestamp(0L, 0),
          Timestamp(0L, 0),
          Timestamp(0L, 0),
          true,
          smithy4s.example.Numbers.TWO
        )
      )

    val expected =
      "dummy-path" :: "example with spaces, %, / and \\" :: "10" :: "1970-01-01T00:00:00Z" :: "1970-01-01T00:00:00Z" :: "0" :: "Thu, 01 Jan 1970 00:00:00 GMT" :: "true" :: "2" :: Nil

    expect.eql(result, expected)
  }

  test("Write PathParams for a struct schema") {
    val schema = struct
      .genericArity(
        string.required[Unit]("label", _ => "example"),
        string.required[Unit]("secondLabel", _ => "example2")
      )(_ => ())
      .addHints(
        Http(
          method = NonEmptyString("GET"),
          uri = NonEmptyString("/{label}/const/{secondLabel}"),
          code = 200
        )
      )
    val result = SchemaVisitorPathEncoder(schema)
      .map(_.encode(()))

    expect.eql(
      result,
      Some(List("example", "const", "example2"))
    )
  }

  test("Write PathParams for a struct schema - URI ending with greedy label") {
    val schema = struct
      .genericArity(
        string.required[Unit]("label", _ => "example"),
        string.required[Unit]("greedyLabel", _ => "example2/with/slashes")
      )(_ => ())
      .addHints(
        Http(
          method = NonEmptyString("GET"),
          uri = NonEmptyString("/{label}/const/{greedyLabel+}"),
          code = 200
        )
      )
    val result = SchemaVisitorPathEncoder(schema)
      .map(_.encode(()))

    expect.eql(
      result,
      Some(List("example", "const", "example2", "with", "slashes"))
    )
  }

  test("Write PathParams for a simple string") {
    expect.eql(
      util.simpleString.map(_.encode("example")),
      Some(List("example"))
    )
  }

  test("Write PathParams for a byte") {
    expect.eql(
      util.encodePathAs(byte).map(_.encode(42)),
      Some(List("42"))
    )
  }

  test("Write PathParams for an int") {
    expect.eql(
      util.encodePathAs(int).map(_.encode(42)),
      Some(List("42"))
    )
  }

  test("Write PathParams for a double") {
    val expected = Some(List {
      if (Platform.isJS) "42" else "42.0"
    })

    expect.eql(
      util.encodePathAs(double).map(_.encode(42.0)),
      expected
    )
  }

  test("Write PathParams for a boolean") {
    expect.eql(
      util.encodePathAs(boolean).map(_.encode(true)),
      Some(List("true"))
    )
  }

  test("Write PathParams for a string with special characters unencoded") {
    val input = "example with all kinds of strange characters / \\ & "

    expect.eql(
      util.simpleString.map(_.encode(input)),
      Some(List(input))
    )
  }

  test(
    "Write PathParams for a string greedily by splitting it on /, unencoded"
  ) {
    val input = "example/with/slashes and spaces"

    expect.eql(
      util.simpleString.map(_.encodeGreedy(input)),
      Some(List("example", "with", "slashes and spaces"))
    )
  }

  test("Write PathParams for unit as None") {
    expect.eql(
      util.encodePathAs(unit).map(_.encode(())),
      None
    )
  }

}
