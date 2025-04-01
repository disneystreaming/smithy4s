/*
 *  Copyright 2021-2025 Disney Streaming
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
import smithy4s.schema.Schema
import smithy.api
// import smithy4s.schema.Field

final class MetadataDecoderSpec extends FunSuite {
  test("Optional header") {
    case class Foo(deviceType: Option[String])
    val schema =
      Schema
        .struct(
          Schema.string
            .optional[Foo]("deviceType", _.deviceType)
            .addHints(api.HttpHeader("x-device-type"))
            .addHints(api.Input())
        )(Foo(_))
        .addHints(smithy4s.internals.InputOutput.Input.widen)

    val decoder = Metadata.Decoder.fromSchema(schema)
    val result = decoder.decode(Metadata())

    assertEquals(result, Right(Foo(None)))
  }

  test("Optional bijection header") {
    case class Foo(name: Option[String])
    val schema: Schema[Foo] = {
      val field = Schema.string.option
        .biject[Option[String]](identity[Option[String]](_))(identity(_))
        .required[Foo]("name", _.name)
        .addHints(smithy.api.HttpHeader("X-Name"))
      Schema
        .struct(field)(Foo(_))
        .addHints(smithy4s.internals.InputOutput.Input.widen)
    }

    val decoder = Metadata.Decoder.fromSchema(schema)
    val result = decoder.decode(Metadata())

    assertEquals(result, Right(Foo(None)))
  }
}
