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

import munit._
import smithy4s.Document
import smithy4s.Schema
import smithy4s.example.SampleOpenUnion
import smithy4s.Blob
import smithy4s.example.SampleOpenDiscriminatedUnion
import smithy4s.example.StructForDiscrimination
import org.scalacheck.Arbitrary
import org.scalacheck.Prop.forAll
import smithy4s.example.OnlyUnknownOpenUnion
import smithy4s.example.OnlyUnknownDiscriminatedOpenUnion
import smithy4s.example.RecursiveOpenUnion
import smithy4s.example.RecursiveDiscriminatedOpenUnion
import smithy4s.example.HasRecursiveDiscriminatedOpenUnion

class OpenUnionJsonSpec() extends ScalaCheckSuite {

  private val genDocument =
    smithy4s.scalacheck.SchemaVisitorGen.apply(Schema.document)

  private val genDocumentMap =
    smithy4s.scalacheck.SchemaVisitorGen.apply(
      Schema.map(Schema.string, Schema.document)
    )

  test("open tagged union - decoding still fails if no tag is present") {
    assert(Json.read[SampleOpenUnion](Blob("{}")).isLeft)
  }

  test("open tagged union - known tags decode normally") {
    roundtripTest(Blob("""{"u":{}}"""), SampleOpenUnion.u())
    roundtripTest(
      Blob("""{"str":"hello"}"""),
      SampleOpenUnion.str("hello")
    )
  }

  test("open tagged union - unknown tags can be roundtripped") {
    val stringCase = Document.obj(
      "brand-new-member" -> Document.fromString("oh wow i'm a string")
    )
    roundtripTest(
      Json.writeDocumentAsBlob(stringCase),
      SampleOpenUnion.unknown(stringCase)
    )

    val objectCase = Document.obj(
      "brand-new-obj-member" -> Document.obj(
        "inner-key" -> Document.fromInt(42)
      )
    )
    roundtripTest(
      Json.writeDocumentAsBlob(objectCase),
      SampleOpenUnion.unknown(objectCase)
    )
  }

  test(
    "open tagged union - if the key used by the unknown member appears, it still roundtrips"
  ) {
    val input = Document.obj("unknown" -> Document.obj())
    roundtripTest(
      Json.writeDocumentAsBlob(input),
      SampleOpenUnion.unknown(input)
    )
  }

  test(
    "open tagged union with only an unknown member - unknown tags can be roundtripped"
  ) {
    forAll(genDocument, Arbitrary.arbitrary[String]) { (document, tag) =>
      val input = document.nest(tag)

      roundtripTest(
        Json.writeDocumentAsBlob(input),
        OnlyUnknownOpenUnion.unknown(input)
      )
    }
  }

  test("recursive open union - inner unknown case") {
    val inner = Document.obj("brand-new-member" -> Document.fromString("foo"))
    val input = Document.obj("rec" -> inner)

    roundtripTest(
      Json.writeDocumentAsBlob(input),
      RecursiveOpenUnion.rec(RecursiveOpenUnion.unknown(inner))
    )
  }

  test(
    "open discriminated union - decoding still fails if the discriminator key is missing"
  ) {
    assert(
      Json
        .read[SampleOpenDiscriminatedUnion](
          Blob("""{"ignoredKey": "foo"}""")
        )
        .isLeft
    )
  }

  test("open discriminated union - known tags decode normally") {
    roundtripTest(
      Blob("""{"type":"u"}"""),
      SampleOpenDiscriminatedUnion.u()
    )
    roundtripTest(
      Blob("""{"type":"s","str":"hello"}"""),
      SampleOpenDiscriminatedUnion.s(StructForDiscrimination("hello"))
    )
  }

  test("open discriminated union - unknown tags can be roundtripped") {
    val stringCase = Document.obj(
      "type" -> Document.fromString("brand-new-member"),
      "extra" -> Document.fromString("oh wow i'm a string")
    )

    roundtripTest(
      Json.writeDocumentAsBlob(stringCase),
      SampleOpenDiscriminatedUnion.unknown(stringCase)
    )

    val objectCase =
      Document.obj(
        "type" -> Document.fromString("brand-new-obj-member"),
        "inner-key" -> Document.fromInt(42)
      )

    roundtripTest(
      Json.writeDocumentAsBlob(objectCase),
      SampleOpenDiscriminatedUnion.unknown(objectCase)
    )
  }

  test(
    "open discriminated union - if the key used by the unknown member appears, it still roundtrips"
  ) {
    val input = Document.obj(
      "type" -> Document.fromString("unknown"),
      "extra" -> Document.obj()
    )
    roundtripTest(
      Json.writeDocumentAsBlob(input),
      SampleOpenDiscriminatedUnion.unknown(input)
    )
  }

  test(
    "open discriminated union with only an unknown member - unknown tags can be roundtripped"
  ) {
    forAll(genDocumentMap, Arbitrary.arbitrary[String]) { (documentKeys, tag) =>
      val input =
        Document.DObject(documentKeys + ("type" -> Document.fromString(tag)))

      roundtripTest(
        Json.writeDocumentAsBlob(input),
        OnlyUnknownDiscriminatedOpenUnion.unknown(input)
      )
    }
  }

  test("recursive open discriminated union - inner unknown case") {
    val inner = Document.obj(
      "type" -> Document.fromString("brand-new-member"),
      "other" -> Document.fromString("foo")
    )

    val input =
      Document.obj(
        "type" -> Document.fromString("rec"),
        "rec" -> inner
      )

    roundtripTest(
      Json.writeDocumentAsBlob(input),
      RecursiveDiscriminatedOpenUnion.rec(
        HasRecursiveDiscriminatedOpenUnion(
          RecursiveDiscriminatedOpenUnion.unknown(inner)
        )
      )
    )
  }

  private def roundtripTest[T: Schema](
      input: Blob,
      expectedOutput: T
  )(implicit
      loc: Location
  ) = {
    val decoded = Json.read[T](input)
    assertEquals(
      decoded,
      Right(expectedOutput),
      clue = "decoded value is not the same"
    )
    val encoded = Json.writeBlob(expectedOutput)
    assertEquals(encoded, input, clue = "roundtripped encoding is not the same")
  }

}
