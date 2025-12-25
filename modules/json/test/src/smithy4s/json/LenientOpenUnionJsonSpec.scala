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

package smithy4s.json

import smithy4s.Blob
import smithy4s.Document
import smithy4s.Schema
import smithy4s.codecs.PayloadError
import smithy4s.codecs.PayloadPath
import smithy4s.example.Foo
import smithy4s.example.SampleOpenUnion
import smithy4s.expect

class LenientOpenUnionJsonSpec extends OpenUnionJsonSpec {

  val payloadCompiler =
    smithy4s.json.internals.JsonPayloadCodecCompilerImpl.defaultJsonPayloadCodecCompiler
      .configureJsoniterCodecCompiler(
        _.withLenientTaggedUnionDecoding
      )

  override def read[A: Schema](blob: Blob): Either[PayloadError, A] =
    payloadCompiler.decoders.fromSchema(Schema[A]).decode(blob)
  override def write[A: Schema](a: A): Blob =
    payloadCompiler.encoders.fromSchema(Schema[A]).encode(a)

  test("lenient open union - known case") {
    val a = Document.obj(
      "str" -> Document.fromString("value"),
      "u" -> Document.nullDoc,
      "other" -> Document.nullDoc
    )

    expect.same(
      read[SampleOpenUnion](writeDocumentAsBlob(a)),
      Right(SampleOpenUnion.str("value"))
    )
  }

  test("lenient open union - fail to decode multiple unknown keys") {
    matches(read[SampleOpenUnion](Blob("""{"foo": "bar", "baz": "qux"}"""))) {
      case Left(ex) =>
        expect(
          ex.getMessage.contains("""Expected a single non-null value""")
        )
    }
  }

  test("lenient union - decoding still fails if invalid tag is present") {
    matches(read[Foo](Blob("""{"foo": "bar"}"""))) { case Left(ex) =>
      expect(ex.getMessage.contains("Expected a single non-null value"))
    }
  }

  test("lenient open union - single unknown case") {
    val a = Document.obj(
      "str" -> Document.nullDoc,
      "u" -> Document.nullDoc,
      "other" -> Document.obj("unknown" -> Document.fromString("case"))
    )

    expect.same(
      read[SampleOpenUnion](writeDocumentAsBlob(a)),
      Right(
        SampleOpenUnion.unknown(
          Document.obj(
            "other" -> Document.obj("unknown" -> Document.fromString("case"))
          )
        )
      )
    )
  }

  test("lenient open union - multiple unknown cases") {
    val a = Document.obj(
      "str" -> Document.nullDoc,
      "u" -> Document.nullDoc,
      "other-1" -> Document.obj("unknown" -> Document.fromString("case")),
      "other-2" -> Document.obj("unknown" -> Document.fromString("case"))
    )

    val result = read[SampleOpenUnion](writeDocumentAsBlob(a))

    matches(result) { case Left(error) =>
      expect.same(
        error.path,
        PayloadPath.root.append("other-2")
      )
      expect(error.message.contains("Expected a single non-null value"))
    }

  }

}
