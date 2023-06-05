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

package smithy4s.http.json

import munit.FunSuite
import smithy.api.JsonName
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

    val capi = codecs(HintMask.empty)

    val codec = capi.compileCodec(schemaWithJsonName)
    val encodedString = new String(capi.writeToArray(codec, "test"))

    assertEquals(encodedString, """{"a":"test"}""")
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

    val capi = codecs(HintMask.empty)

    val codec = capi.compileCodec(schemaWithRequiredField)

    val decoded = capi.decodeFromByteArray(codec, """{}""".getBytes())

    assert(decoded.isLeft)
  }
  
  test("passing empty string to codec should not fail decoding") {
    case class A(a: Option[String])
    val schemaWithOptionalField =
      Schema
        .struct[A]
        .apply(
          Schema.string
            .optional[A]("a", _.a)
        )(A.apply(_))
    val capi = codecs(HintMask.empty)
    val codec = capi.compileCodec(schemaWithOptionalField)
    val decoded = capi.decodeFromByteArray(codec, "".getBytes())
    assert(decoded.isRight)
  }

  test("if all fields are empty , an empty json object should be written") {
    case class A(a: Option[String], b: Option[String])
    val schemaWithOptionalField =
      Schema
        .struct[A]
        .apply(
          Schema.string
            .optional[A]("a", _.a),
          Schema.string
            .optional[A]("b", _.b)
        )(A.apply(_, _))
    val capi = codecs(HintMask.empty)
    val codec = capi.compileCodec(schemaWithOptionalField)
    val encoded = capi.writeToArray(codec, A(None, None))
    assertEquals(new String(encoded), "{}")
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

    val capi = codecs(HintMask.empty, explicitNullEncoding = true)

    val codec = capi.compileCodec(schemaWithJsonName)
    val encodedString = new String(capi.writeToArray(codec, None))

    assertEquals(encodedString, """{"a":null}""")
  }

  test(
    "explicit nulls should be parsable regardless of explicitNullEncoding setting"
  ) {
    List(true, false).foreach { nullEncoding =>
      val schemaWithJsonName = Schema
        .struct[Option[String]]
        .apply(
          Schema.string
            .optional[Option[String]]("a", identity)
        )(identity)

      val capi =
        codecs(HintMask.empty, explicitNullEncoding = nullEncoding)

      val codec = capi.compileCodec(schemaWithJsonName)
      val decoded = capi.decodeFromByteArray(codec, """{"a":null}""".getBytes())

      assertEquals(decoded, Right(None))
    }
  }

}
