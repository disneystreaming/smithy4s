/*
 *  Copyright 2021 Disney Streaming
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

package smithy4s.http.internals

import smithy4s.Timestamp
import smithy4s.example.DummyServiceGen.DummyPath
import smithy4s.example.PathParams
import smithy4s.http.HttpEndpoint
import smithy4s.http.PathSegment
import smithy4s.Schema
import smithy.api.Http
import smithy.api.NonEmptyString

object PathSpec extends weaver.FunSuite {
  import smithy4s.syntax._
  object util {

    def encodePathAs[A](schema: Schema[A]): Option[PathEncode[A]] = schema
      .withHints(
        Http(
          method = NonEmptyString("GET"),
          uri = NonEmptyString("/{label}")
        )
      )
      .compile(SchematicPathEncoder)
      .get

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

  test("Write PathParams for DummyPath") {
    val result = HttpEndpoint
      .cast(
        DummyPath
      )
      .get
      .path(
        PathParams(
          "example with spaces, %, / and \\",
          10,
          Timestamp.fromEpochSecond(0L),
          Timestamp.fromEpochSecond(0L),
          Timestamp.fromEpochSecond(0L),
          Timestamp.fromEpochSecond(0L),
          true
        )
      )

    val expected = if (weaver.Platform.isJS) {
      "dummy-path" :: "example with spaces, %, / and \\" :: "10" :: "1970-01-01T00:00:00.000Z" :: "1970-01-01T00:00:00.000Z" :: "0" :: "Thu, 01 Jan 1970 00:00:00 GMT" :: "true" :: Nil
    } else {
      "dummy-path" :: "example with spaces, %, / and \\" :: "10" :: "1970-01-01T00:00:00Z" :: "1970-01-01T00:00:00Z" :: "0" :: "Thu, 01 Jan 1970 00:00:00 GMT" :: "true" :: Nil
    }

    assert.eql(result, expected)
  }

  test("Write PathParams for a struct schema") {
    val result = struct(
      Vector(
        string.required[Unit]("label", _ => "example"),
        string.required[Unit]("secondLabel", _ => "example2")
      )
    )(_ => ())
      .withHints(
        Http(
          method = NonEmptyString("GET"),
          uri = NonEmptyString("/{label}/const/{secondLabel}")
        )
      )
      .compile(SchematicPathEncoder)
      .get
      .map(_.encode(()))

    assert.eql(
      result,
      Some(List("example", "const", "example2"))
    )
  }

  test("Write PathParams for a struct schema - URI ending with greedy label") {
    val result = struct(
      Vector(
        string.required[Unit]("label", _ => "example"),
        string.required[Unit]("greedyLabel", _ => "example2/with/slashes")
      )
    )(_ => ())
      .withHints(
        Http(
          method = NonEmptyString("GET"),
          uri = NonEmptyString("/{label}/const/{greedyLabel+}")
        )
      )
      .compile(SchematicPathEncoder)
      .get
      .map(_.encode(()))

    assert.eql(
      result,
      Some(List("example", "const", "example2", "with", "slashes"))
    )
  }

  test("Write PathParams for a simple string") {
    assert.eql(
      util.simpleString.map(_.encode("example")),
      Some(List("example"))
    )
  }

  test("Write PathParams for an int") {
    assert.eql(
      util.encodePathAs(int).map(_.encode(42)),
      Some(List("42"))
    )
  }

  test("Write PathParams for a double") {
    val expected = Some(List {
      if (weaver.Platform.isJS) "42" else "42.0"
    })

    assert.eql(
      util.encodePathAs(double).map(_.encode(42.0)),
      expected
    )
  }

  test("Write PathParams for a boolean") {
    assert.eql(
      util.encodePathAs(boolean).map(_.encode(true)),
      Some(List("true"))
    )
  }

  test("Write PathParams for a string with special characters unencoded") {
    val input = "example with all kinds of strange characters / \\ & "

    assert.eql(
      util.simpleString.map(_.encode(input)),
      Some(List(input))
    )
  }

  test(
    "Write PathParams for a string greedily by splitting it on /, unencoded"
  ) {
    val input = "example/with/slashes and spaces"

    assert.eql(
      util.simpleString.map(_.encodeGreedy(input)),
      Some(List("example", "with", "slashes and spaces"))
    )
  }

  test("Write PathParams for unit as None") {
    assert.eql(
      util.encodePathAs(unit).map(_.encode(())),
      None
    )
  }

}
