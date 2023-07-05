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

package smithy4s.json

import munit.FunSuite
import smithy.api.JsonName
import smithy4s.Blob
import smithy4s.schema.Schema
import smithy4s.HintMask

class JsonCodecApiTests extends FunSuite {

  test(
    "codecs with an empty hint mask should not be affected by format hints"
  ) {
    val schemaWithJsonName = Schema
      .struct[String]
      .apply(
        Schema.string
          .addHints(JsonName("b"))
          .required[String]("a", identity)
      )(identity)

    val capi = Json.payloadCodecs.withJsoniterCodecCompiler(
      Json.jsoniter.withHintMask(HintMask.empty)
    )

    val codec = capi.fromSchema(schemaWithJsonName)
    val encoded = codec.writer.encode("test")

    assertEquals(encoded, Blob("""{"a":"test"}"""))
  }

  test(
    "struct codec with a required field should return a Left when the field is missing"
  ) {
    val schemaWithRequiredField =
      Schema
        .struct[String]
        .apply(
          Schema.string
            .required[String]("a", identity)
        )(identity)

    val codec = Json.payloadCodecs.fromSchema(schemaWithRequiredField)
    val decoded = codec.reader.decode(Blob("{}"))

    assert(decoded.isLeft)
  }

  test(
    "explicit nulls should be used when set"
  ) {
    val schemaWithJsonName = Schema
      .struct[Option[String]]
      .apply(
        Schema.string
          .optional[Option[String]]("a", identity)
      )(identity)

    val capi = Json.payloadCodecs.withJsoniterCodecCompiler(
      Json.jsoniter.withExplicitNullEncoding(true)
    )

    val codec = capi.fromSchema(schemaWithJsonName)
    val encoded = codec.writer.encode(None)

    assertEquals(encoded, Blob("""{"a":null}"""))
  }

  test(
    "explicit nulls should be parsable regardless of explicitNullEncoding setting"
  ) {
    val withoutNulls = Json.payloadCodecs
    val withNulls = Json.payloadCodecs.withJsoniterCodecCompiler(
      Json.jsoniter.withExplicitNullEncoding(true)
    )

    List(withoutNulls, withNulls).foreach { capi =>
      val schemaWithJsonName = Schema
        .struct[Option[String]]
        .apply(
          Schema.string
            .optional[Option[String]]("a", identity)
        )(identity)

      val codec = capi.fromSchema(schemaWithJsonName)
      val decoded = codec.reader.decode(Blob("""{"a":null}"""))

      assertEquals(decoded, Right(None))
    }
  }

}
