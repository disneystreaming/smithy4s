/*
 *  Copyright 2021-2024 Disney Streaming
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

package smithy4s.http

import munit._
import smithy4s.schema.OperationSchema
import smithy4s.schema.Schema
import smithy4s._
import smithy.api

final class HttpRequestSpec extends FunSuite {

  test("host prefix") {
    case class Foo(foo: String)
    val schema =
      Schema.struct(Schema.string.required[Foo]("foo", _.foo))(Foo(_))
    val endpointHint =
      api.Endpoint(hostPrefix = api.NonEmptyString("test.{foo}-other."))
    val opSchema = OperationSchema(
      ShapeId("test", "Test"),
      Hints(endpointHint),
      schema,
      None,
      Schema.unit,
      None,
      None
    )

    val writer = HttpRequest.Writer.hostPrefix[String, Foo](opSchema)

    val uri = HttpUri(
      HttpUriScheme.Https,
      "example.com",
      None,
      IndexedSeq.empty,
      Map.empty,
      None
    )
    val request = HttpRequest(HttpMethod.GET, uri, Map.empty, "")
    val resultUri = writer.write(request, Foo("hello")).uri
    assertEquals(resultUri, uri.copy(host = "test.hello-other.example.com"))
  }

}
