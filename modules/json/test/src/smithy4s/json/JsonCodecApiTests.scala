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

package smithy4s.json

import munit.FunSuite
import smithy.api.JsonName
import smithy.api.Length
import smithy4s.Blob
import smithy4s.HintMask
import smithy4s.Nullable
import smithy4s.RefinementProvider
import smithy4s.schema.FieldFilter
import smithy4s.schema.Schema

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

    val encoded = capi.encoders.fromSchema(schemaWithJsonName).encode("test")

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

    val codec = Json.payloadDecoders.fromSchema(schemaWithRequiredField)
    val decoded = codec.decode(Blob("{}"))

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
      Json.jsoniter.withFieldFilter(FieldFilter.EncodeAll)
    )

    val codec = capi.encoders.fromSchema(schemaWithJsonName)
    val encoded = codec.encode(None)

    assertEquals(encoded, Blob("""{"a":null}"""))
  }

  test(
    "explicit nulls should be parsable regardless of fieldFilter setting"
  ) {
    val withoutNulls = Json.payloadCodecs
    val withNulls = Json.payloadCodecs.withJsoniterCodecCompiler(
      Json.jsoniter.withFieldFilter(FieldFilter.EncodeAll)
    )

    List(withoutNulls, withNulls).foreach { capi =>
      val schemaWithJsonName = Schema
        .struct[Option[String]]
        .apply(
          Schema.string
            .optional[Option[String]]("a", identity)
        )(identity)

      val codec = capi.decoders.fromSchema(schemaWithJsonName)
      val decoded = codec.decode(Blob("""{"a":null}"""))

      assertEquals(decoded, Right(None))
    }
  }

  test(
    "schemas backed by an OptionSchema should be treated same as OptionSchema itself"
  ) {
    case class OptionalLike[+A](underlying: Option[A])
    case class Options(
        justOption: Option[String],
        bijectedOption: OptionalLike[String],
        refinedOption: Option[String],
        recursive: Option[Options],
        optionalNullable: Option[Nullable[String]]
    )

    lazy val schema: Schema[Options] = Schema.recursive {
      Schema
        .struct[Options]
        .apply(
          Schema.string.optional[Options]("justOption", _.justOption),
          Schema.string.option
            .biject(OptionalLike(_))(_.underlying)
            .field[Options]("bijectedOption", _.bijectedOption),
          Schema.string.option
            .validated(Length(max = Some(10)))(
              RefinementProvider.lengthConstraint(_.fold(0)(_.length))
            )
            .field[Options]("refinedOption", _.justOption),
          schema.optional[Options]("recursive", _.recursive),
          Schema.string.nullable
            .optional[Options]("optionalNullable", _.optionalNullable)
        )(Options.apply)
    }

    val capi = Json.payloadCodecs.withJsoniterCodecCompiler(
      Json.jsoniter.withFieldFilter(FieldFilter.SkipUnsetOptions)
    )

    val encoder = capi.encoders.fromSchema(schema)

    assertEquals(
      encoder
        .encode(
          Options(
            justOption = None,
            bijectedOption = OptionalLike(None),
            refinedOption = None,
            recursive = None,
            optionalNullable = None
          )
        )
        .toUTF8String,
      Blob("{}").toUTF8String
    )

    assertEquals(
      encoder
        .encode(
          Options(
            justOption = Some("a"),
            bijectedOption = OptionalLike(Some("a")),
            refinedOption = Some("a"),
            recursive = Some(
              Options(
                justOption = None,
                bijectedOption = OptionalLike(None),
                refinedOption = None,
                recursive = None,
                optionalNullable = None
              )
            ),
            optionalNullable = Some(Nullable.Null)
          )
        )
        .toUTF8String,
      Blob(
        """{"justOption":"a","bijectedOption":"a","refinedOption":"a","recursive":{},"optionalNullable":null}"""
      ).toUTF8String
    )
  }

}
