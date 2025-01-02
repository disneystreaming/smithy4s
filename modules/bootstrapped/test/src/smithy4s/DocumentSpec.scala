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

package smithy4s

import smithy.api.JsonName
import smithy.api.Default
import smithy4s.example.IntList
import alloy.Discriminated
import alloy.JsonUnknown
import munit._
import smithy4s.example.DefaultNullsOperationOutput
import alloy.Untagged
import smithy4s.example.TimestampOperationInput
import scala.util.Try

class DocumentSpec() extends FunSuite {

  test("Recursive document codecs should not blow up the stack") {
    val recursive: IntList = IntList(1, Some(IntList(2, Some(IntList(3)))))

    val document = Document.encode(recursive)
    import Document._
    val expectedDocument =
      obj(
        "head" -> fromInt(1),
        "tail" -> obj(
          "head" -> fromInt(2),
          "tail" -> obj("head" -> fromInt(3))
        )
      )

    val roundTripped = Document.decode[IntList](document)

    expect(document == expectedDocument)
    expect(roundTripped == Right(recursive))
  }

  import smithy4s.schema.Schema._
  implicit val tupleIntStringSchema: Schema[(Int, String)] =
    Schema.tuple(
      int.addMemberHints(JsonName("int")),
      string.addMemberHints(JsonName("_string"))
    )

  implicit val eitherIntStringSchema: Schema[Either[Int, String]] =
    Schema.either(int, string.addMemberHints(JsonName("_string")))

  case class Foo(str: String)
  case class Bar(str: String, int: Int)

  implicit val eitherFooBarSchema: Schema[Either[Foo, Bar]] = {
    val left = struct(string.required[Foo]("str", _.str))(Foo.apply)
    val right = struct(
      string.required[Bar]("str", _.str).addHints(JsonName("barStr")),
      int.required[Bar]("int", _.int)
    )(Bar.apply)
      .addMemberHints(JsonName("barBar"))

    Schema
      .either(left, right)
      .addHints(
        Discriminated("type")
      )
  }

  case class Baz()

  implicit val eitherFooBazSchema: Schema[Either[Foo, Baz]] = {
    val left = struct(string.required[Foo]("str", _.str))(Foo.apply)

    val right = constant(Baz())

    Schema
      .either(left, right)
      .addHints(
        Discriminated("type")
      )
  }

  test("jsonName is handled correctly on structures") {
    val intAndString: (Int, String) = (1, "hello")

    val document = Document.encode(intAndString)
    import Document._
    val expectedDocument =
      obj(
        "int" -> fromInt(1),
        "_string" -> fromString("hello")
      )

    val roundTripped = Document.decode[(Int, String)](document)

    expect.same(document, expectedDocument)
    expect.same(roundTripped, Right(intAndString))
  }

  test("jsonName is handled correctly on unions") {
    val intOrString: Either[Int, String] = Right("hello")

    val document = Document.encode(intOrString)
    import Document._
    val expectedDocument =
      obj(
        "_string" -> fromString("hello")
      )

    val roundTripped = Document.decode[Either[Int, String]](document)

    expect.same(document, expectedDocument)
    expect.same(roundTripped, Right(intOrString))
  }

  test("discriminated unions encoding") {
    val fooOrBar: Either[Foo, Bar] = Right(Bar("hello", 2022))

    val document = Document.encode(fooOrBar)
    import Document._
    val expectedDocument =
      obj(
        "barStr" -> fromString("hello"),
        "int" -> fromInt(2022),
        "type" -> fromString("barBar")
      )

    val roundTripped = Document.decode[Either[Foo, Bar]](document)

    expect.same(document, expectedDocument)
    expect.same(roundTripped, Right(fooOrBar))
  }

  test("untagged unions encoding") {
    implicit val eitherSchema: Schema[Either[Long, String]] = {
      val left = Schema.long
      val right = Schema.string

      Schema
        .either(left, right)
        .addHints(
          Untagged()
        )
    }
    val longOrString: Either[Long, String] = Right("hello")

    val document = Document.encode(longOrString)
    import Document._
    val expectedDocument =
      Document.fromString("hello")

    val roundTripped = Document.decode[Either[Long, String]](document)

    expect.same(document, expectedDocument)
    expect.same(roundTripped, Right(longOrString))
  }

  test("discriminated unions encoding - empty structure alternative") {
    val fooOrBaz: Either[Foo, Baz] = Right(Baz())

    val document = Document.encode(fooOrBaz)
    import Document._
    val expectedDocument =
      obj(
        "type" -> fromString("right")
      )

    val roundTripped = Document.decode[Either[Foo, Baz]](document)

    expect.same(document, expectedDocument)
    expect.same(roundTripped, Right(fooOrBaz))
  }

  test("integer based enum") {
    import smithy4s.example._
    val faceCard: FaceCard = FaceCard.ACE
    val document = Document.encode(faceCard)
    import Document._
    val expectedDocument = DNumber(faceCard.intValue)

    val roundTripped = Document.decode[FaceCard](document)

    assertEquals(document, expectedDocument)
    assertEquals(roundTripped, Right(faceCard))
  }

  test("open integer based enum - known") {
    import smithy4s.example._
    val one: OpenIntEnumTest = OpenIntEnumTest.ONE
    val document = Document.encode(one)
    import Document._
    val expectedDocument = DNumber(OpenIntEnumTest.ONE.intValue)

    val roundTripped = Document.decode[OpenIntEnumTest](document)

    assertEquals(document, expectedDocument)
    assertEquals(roundTripped, Right(one))
  }

  test("open integer based enum - unknown") {
    import smithy4s.example._
    val unknown: OpenIntEnumTest = OpenIntEnumTest.$Unknown(202)
    val document = Document.encode(unknown)
    import Document._
    val expectedDocument = DNumber(202)

    val roundTripped = Document.decode[OpenIntEnumTest](document)

    assertEquals(document, expectedDocument)
    assertEquals(roundTripped, Right(unknown))
  }

  test("open integer based enum key") {
    import smithy4s.example._
    implicit val mapSchema: Schema[Map[OpenIntEnumTest, Int]] =
      map(OpenIntEnumTest.schema, int)

    val mapTest =
      Map(OpenIntEnumTest.ONE -> 1, OpenIntEnumTest.$Unknown(100) -> 2)
    val document = Document.encode(mapTest)
    import Document._
    val expectedDocument =
      obj(
        "1" -> fromInt(1),
        "100" -> fromInt(2)
      )

    val roundTripped = Document.decode[Map[OpenIntEnumTest, Int]](document)

    assertEquals(document, expectedDocument)
    assertEquals(roundTripped, Right(mapTest))
  }

  test("open string based enum - known") {
    import smithy4s.example._
    val one: OpenEnumTest = OpenEnumTest.ONE
    val document = Document.encode(one)
    import Document._
    val expectedDocument = DString(OpenEnumTest.ONE.value)

    val roundTripped = Document.decode[OpenEnumTest](document)

    assertEquals(document, expectedDocument)
    assertEquals(roundTripped, Right(one))
  }

  test("open string based enum - unknown") {
    import smithy4s.example._
    val unknown: OpenEnumTest = OpenEnumTest.$Unknown("test")
    val document = Document.encode(unknown)
    import Document._
    val expectedDocument = DString("test")

    val roundTripped = Document.decode[OpenEnumTest](document)

    assertEquals(document, expectedDocument)
    assertEquals(roundTripped, Right(unknown))
  }

  test("open string based enum key") {
    import smithy4s.example._
    implicit val mapSchema: Schema[Map[OpenEnumTest, Int]] =
      map(OpenEnumTest.schema, int)

    val mapTest =
      Map(OpenEnumTest.ONE -> 1, OpenEnumTest.$Unknown("test") -> 2)
    val document = Document.encode(mapTest)
    import Document._
    val expectedDocument =
      obj(
        "ONE" -> fromInt(1),
        "test" -> fromInt(2)
      )

    val roundTripped = Document.decode[Map[OpenEnumTest, Int]](document)

    assertEquals(document, expectedDocument)
    assertEquals(roundTripped, Right(mapTest))
  }

  case class DefTest(int: Int, str: String)
  implicit val withDefaultsSchema: Schema[DefTest] = {
    val i = int
      .required[DefTest]("int", _.int)
      .addHints(Default(Document.fromInt(11)))
    val s =
      string
        .required[DefTest]("str", _.str)
        .addHints(Default(Document.fromString("test")))
    struct(i, s)(DefTest.apply)
  }

  test("defaults should be applied when fields missing") {
    import Document._

    val expectedDecoded = DefTest(11, "test")

    val fromEmpty = Document.decode[DefTest](obj())

    expect.same(fromEmpty, Right(expectedDecoded))
  }

  test("defaults should not be applied when field is provided") {
    val defTest = DefTest(12, "test2")

    val document = Document.encode(defTest)
    import Document._
    val expectedDocument =
      obj(
        "int" -> fromInt(12),
        "str" -> fromString("test2")
      )

    val roundTripped = Document.decode[DefTest](document)

    expect.same(document, expectedDocument)
    expect.same(roundTripped, Right(defTest))
  }

  test("encoding maps keyed with newtypes should not change the encoding") {
    case class Key(string: String)
    val keySchema = bijection(string, Key(_), (_: Key).string)
    implicit val mapSchema: Schema[Map[Key, Int]] = map(keySchema, int)

    val mapTest = Map(Key("hello") -> 1, Key("world") -> 2)

    val document = Document.encode(mapTest)
    import Document._
    val expectedDocument =
      obj(
        "hello" -> fromInt(1),
        "world" -> fromInt(2)
      )

    val roundTripped = Document.decode[Map[Key, Int]](document)

    expect.same(document, expectedDocument)
    expect.same(roundTripped, Right(mapTest))
  }

  test(
    "encoding maps keyed with validated types should not change the encoding"
  ) {
    val keySchema = int.validated(smithy.api.Range(None, Some(BigDecimal(3))))
    implicit val mapSchema: Schema[Map[Int, Int]] = map(keySchema, int)

    val mapTest = Map(1 -> 1, 2 -> 2)

    val document = Document.encode(mapTest)
    import Document._
    val expectedDocument =
      obj(
        "1" -> fromInt(1),
        "2" -> fromInt(2)
      )

    val roundTripped = Document.decode[Map[Int, Int]](document)

    expect.same(document, expectedDocument)
    expect.same(roundTripped, Right(mapTest))
  }

  test("encoding NaN") {
    // The Document type cannot hold a `NaN` value since it uses BigDecimal to hold numeric values
    // this test exists to show this. For the same reason, a test on decoding from `NaN` is not necessary
    // or possible.
    implicit val schema: Schema[Double] =
      double.validated(smithy.api.Range(None, Some(BigDecimal(3))))

    val in = Double.NaN
    val error = Try(Document.encode(in)).failed.get
    val expectedMessage =
      if (weaver.Platform.isJS || weaver.Platform.isNative)
        "For input string: \"NaN\""
      else
        "Character N is neither a decimal digit number, decimal point, nor \"e\" notation exponential mark."

    expect.same(
      error.getMessage,
      expectedMessage
    )
  }

  test(
    "optional fields for structs should decode Document.DNull"
  ) {
    val optionalFieldSchema =
      Schema
        .struct[Option[String]](
          Schema.string.optional[Option[String]]("test", identity)
        )(identity)

    val decoded = Document.Decoder
      .fromSchema(optionalFieldSchema)
      .decode(Document.obj("test" -> Document.DNull))

    expect.same(decoded, Right(None))

  }

  test(
    "fields marked with @required and @default should always be encoded"
  ) {
    val requiredFieldSchema =
      Schema
        .struct[String](
          Schema.string
            .required[String]("test", identity)
            .addHints(smithy.api.Default(Document.fromString("default")))
        )(identity)

    val encoded = Document.Encoder
      .fromSchema(requiredFieldSchema)
      .encode("default")

    expect.same(encoded, Document.obj("test" -> Document.fromString("default")))
  }

  test(
    "fields marked with @default but not @required should be skipped during encoding when matching default"
  ) {
    val fieldSchema =
      Schema
        .struct[String](
          Schema.string
            .field[String]("test", identity)
            .addHints(smithy.api.Default(Document.fromString("default")))
        )(identity)

    val encoded = Document.Encoder
      .fromSchema(fieldSchema)
      .encode("default")

    expect.same(encoded, Document.obj())
  }

  test("document encoder - all default") {
    val result = Document.Encoder
      .fromSchema(DefaultNullsOperationOutput.schema)
      .encode(DefaultNullsOperationOutput())

    expect.same(
      Document.obj(
        "requiredWithDefault" -> Document.fromString("required-default"),
        "requiredHeaderWithDefault" -> Document.fromString(
          "required-header-with-default"
        )
      ),
      result
    )

  }

  test(
    "document encoder - all default values + explicit defaults encoding = true"
  ) {
    val result = Document.Encoder
      .withExplicitDefaultsEncoding(true)
      .fromSchema(
        DefaultNullsOperationOutput.schema
      )
      .encode(DefaultNullsOperationOutput())
    expect.same(
      Document.obj(
        "optional" -> Document.nullDoc,
        "optionalHeader" -> Document.nullDoc,
        "optionalWithDefault" -> Document.fromString("optional-default"),
        "requiredWithDefault" -> Document.fromString("required-default"),
        "optionalHeaderWithDefault" -> Document.fromString(
          "optional-header-with-default"
        ),
        "requiredHeaderWithDefault" -> Document.fromString(
          "required-header-with-default"
        )
      ),
      result
    )
  }

  test(
    "document encoder - all default values + explicit defaults encoding = false"
  ) {
    val result = Document.Encoder
      .withExplicitDefaultsEncoding(false)
      .fromSchema(
        DefaultNullsOperationOutput.schema
      )
      .encode(DefaultNullsOperationOutput())
    expect.same(
      Document.obj(
        "requiredWithDefault" -> Document.fromString("required-default"),
        "requiredHeaderWithDefault" -> Document.fromString(
          "required-header-with-default"
        )
      ),
      result
    )
  }

  test(
    "document encoder - default values overrides + explicit defaults encoding = true"
  ) {
    val result = Document.Encoder
      .withExplicitDefaultsEncoding(true)
      .fromSchema(DefaultNullsOperationOutput.schema)
      .encode(
        DefaultNullsOperationOutput(
          optional = Some("optional-override"),
          optionalWithDefault = "optional-default-override",
          requiredWithDefault = "required-default-override",
          optionalHeader = Some("optional-header-override"),
          optionalHeaderWithDefault = "optional-header-with-default-override",
          requiredHeaderWithDefault = "required-header-with-default-override"
        )
      )

    expect.same(
      Document.obj(
        "optional" -> Document.fromString("optional-override"),
        "optionalWithDefault" -> Document.fromString(
          "optional-default-override"
        ),
        "requiredWithDefault" -> Document.fromString(
          "required-default-override"
        ),
        "optionalHeader" -> Document.fromString(
          "optional-header-override"
        ),
        "optionalHeaderWithDefault" -> Document.fromString(
          "optional-header-with-default-override"
        ),
        "requiredHeaderWithDefault" -> Document.fromString(
          "required-header-with-default-override"
        )
      ),
      result
    )

  }
  test(
    "document encoder - default values overrides + explicit defaults encoding = false"
  ) {
    val result = Document.Encoder
      .withExplicitDefaultsEncoding(false)
      .fromSchema(DefaultNullsOperationOutput.schema)
      .encode(
        DefaultNullsOperationOutput(
          optional = Some("optional-override"),
          optionalWithDefault = "optional-default-override",
          requiredWithDefault = "required-default-override",
          optionalHeader = Some("optional-header-override"),
          optionalHeaderWithDefault = "optional-header-with-default-override",
          requiredHeaderWithDefault = "required-header-with-default-override"
        )
      )

    expect.same(
      Document.obj(
        "optional" -> Document.fromString("optional-override"),
        "optionalWithDefault" -> Document.fromString(
          "optional-default-override"
        ),
        "requiredWithDefault" -> Document.fromString(
          "required-default-override"
        ),
        "optionalHeader" -> Document.fromString(
          "optional-header-override"
        ),
        "optionalHeaderWithDefault" -> Document.fromString(
          "optional-header-with-default-override"
        ),
        "requiredHeaderWithDefault" -> Document.fromString(
          "required-header-with-default-override"
        )
      ),
      result
    )

  }

  test("Document encoder - timestamp defaults") {
    val result = Document.Encoder
      .withExplicitDefaultsEncoding(false)
      .fromSchema(TimestampOperationInput.schema)
      .encode(TimestampOperationInput())
    expect.same(
      Document.obj(
        "httpDate" -> Document.fromString("Thu, 23 May 2024 10:20:30 GMT"),
        "dateTime" -> Document.fromString("2024-05-23T10:20:30Z"),
        "epochSeconds" -> Document.fromLong(1716459630L)
      ),
      result
    )
  }

  test("Document encoder - timestamp epoch seconds with nanos") {
    val timestampWithNanos =
      Timestamp(1716459630L, 500 * 1000 * 1000 /* half a second */ )
    val result = Document.Encoder
      .withExplicitDefaultsEncoding(false)
      .fromSchema(TimestampOperationInput.schema)
      .encode(
        TimestampOperationInput(
          timestampWithNanos,
          timestampWithNanos,
          timestampWithNanos
        )
      )
    expect.same(
      Document.obj(
        "httpDate" -> Document.fromString("Thu, 23 May 2024 10:20:30.500 GMT"),
        "dateTime" -> Document.fromString("2024-05-23T10:20:30.500Z"),
        "epochSeconds" -> Document.fromBigDecimal(
          BigDecimal("1716459630.500000")
        )
      ),
      result
    )
  }

  test(
    "Document encoder - timestamp epoch seconds uses correct BigDecimal scale"
  ) {
    def check(ts: Timestamp, expectedScale: Int) = {
      val result = Document.Encoder
        .fromSchema(TimestampOperationInput.schema)
        .encode(TimestampOperationInput(ts, ts, ts))
      inside(result) { case Document.DObject(fields) =>
        inside(fields.get("epochSeconds")) {
          case Some(Document.DNumber(bigDecimal)) =>
            expect.same(bigDecimal.scale, expectedScale)
        }
      }
    }
    check(Timestamp(1L, 0), 0)
    check(Timestamp(1L, 500 * 1000 * 1000), 1)
    check(Timestamp(1L, 123 * 1000 * 1000), 3)
  }

  test("Document decoder - timestamps before linux epoch") {
    val doc =
      Document.obj("epochSeconds" -> Document.fromBigDecimal(-0.999999877))
    val result = Document.Decoder
      .fromSchema(TimestampOperationInput.schema)
      .decode(doc)
    expect.same(
      result,
      Right(TimestampOperationInput(epochSeconds = Timestamp(-1, 123)))
    )

  }

  test("Document decoder - timestamp defaults") {
    val doc = Document.obj()
    val result = Document.Decoder
      .fromSchema(TimestampOperationInput.schema)
      .decode(doc)
    val defaultTimestamp = Timestamp(
      year = 2024,
      month = 5,
      day = 23,
      hour = 10,
      minute = 20,
      second = 30
    )
    expect.same(
      Right(
        TimestampOperationInput(
          dateTime = defaultTimestamp,
          httpDate = defaultTimestamp,
          epochSeconds = defaultTimestamp
        )
      ),
      result
    )
  }

  test("Document syntax allows to build documents more concisely") {
    import Document.syntax._

    val niceSyntaxDocument = obj(
      "int" -> 1,
      "boolean" -> true,
      "long" -> 2L,
      "string" -> "hello",
      "nested" -> obj("null" -> nullDoc),
      "array" -> array("one", "two", "three"),
      "fromSchema" -> JsonName("name")
    )

    val expectedDocument = Document.obj(
      "int" -> Document.DNumber(BigDecimal(1)),
      "boolean" -> Document.DBoolean(true),
      "long" -> Document.DNumber(BigDecimal(2)),
      "string" -> Document.DString("hello"),
      "nested" -> Document.DObject(Map("null" -> Document.DNull)),
      "array" -> Document.DArray(
        IndexedSeq(
          Document.DString("one"),
          Document.DString("two"),
          Document.DString("three")
        )
      ),
      "fromSchema" -> Document.DString("name")
    )

    assertEquals(niceSyntaxDocument, expectedDocument)
  }

  case class JsonUnknownExample(
      s: String,
      i: Int,
      others: Map[String, Document]
  )

  object JsonUnknownExample {
    implicit val jsonUnknownExampleSchema: Schema[JsonUnknownExample] = {
      val s = string.required[JsonUnknownExample]("s", _.s)
      val i = int.required[JsonUnknownExample]("i", _.i)
      val others = map(string, document)
        .required[JsonUnknownExample]("others", _.others)
        .addHints(JsonUnknown())
      struct(s, i, others)(JsonUnknownExample.apply)
    }
  }

  object JsonUnknownExampleWithDefault {
    implicit val jsonUnknownExampleSchema: Schema[JsonUnknownExample] = {
      val s = string.required[JsonUnknownExample]("s", _.s)
      val i = int.required[JsonUnknownExample]("i", _.i)
      val others = map(string, document)
        .required[JsonUnknownExample]("others", _.others)
        .addHints(
          JsonUnknown(),
          Default(Document.obj("default" -> Document.fromBoolean(true)))
        )
      struct(s, i, others)(JsonUnknownExample.apply)
    }
  }

  case class JsonUnknownExampleOptional(
      s: String,
      i: Int,
      others: Option[Map[String, Document]]
  )

  object JsonUnknownExampleOptional {
    implicit val jsonUnknownExampleOptionalSchema
        : Schema[JsonUnknownExampleOptional] = {
      val s = string.required[JsonUnknownExampleOptional]("s", _.s)
      val i = int.required[JsonUnknownExampleOptional]("i", _.i)
      val others = map(string, document)
        .optional[JsonUnknownExampleOptional]("others", _.others)
        .addHints(JsonUnknown())
      struct(s, i, others)(JsonUnknownExampleOptional.apply)
    }
  }

  object JsonUnknownExampleOptionalWithDefault {
    implicit val jsonUnknownExampleOptionalSchema
        : Schema[JsonUnknownExampleOptional] = {
      val s = string.required[JsonUnknownExampleOptional]("s", _.s)
      val i = int.required[JsonUnknownExampleOptional]("i", _.i)
      val others = map(string, document)
        .optional[JsonUnknownExampleOptional]("others", _.others)
        .addHints(
          JsonUnknown(),
          Default(Document.obj("default" -> Document.fromBoolean(true)))
        )
      struct(s, i, others)(JsonUnknownExampleOptional.apply)
    }
  }

  test("unknown field decoding: no unknown field in payload") {
    val doc = Document.obj(
      "s" -> Document.fromString("foo"),
      "i" -> Document.fromInt(67)
    )
    val expected = JsonUnknownExample("foo", 67, Map.empty)

    val res = Document.Decoder
      .fromSchema(JsonUnknownExample.jsonUnknownExampleSchema)
      .decode(doc)

    assertEquals(res, Right(expected))
  }

  test("unknown field decoding: no unknown field in payload with default") {
    val doc = Document.obj(
      "s" -> Document.fromString("foo"),
      "i" -> Document.fromInt(67)
    )
    val expected = JsonUnknownExample(
      "foo",
      67,
      Map("default" -> Document.fromBoolean(true))
    )

    val res = Document.Decoder
      .fromSchema(JsonUnknownExampleWithDefault.jsonUnknownExampleSchema)
      .decode(doc)

    assertEquals(res, Right(expected))
  }

  test(
    "unknown field decoding: no unknown field in payload, optional field"
  ) {
    val doc = Document.obj(
      "s" -> Document.fromString("foo"),
      "i" -> Document.fromInt(67)
    )
    val expected = JsonUnknownExampleOptional("foo", 67, None)

    val res = Document.Decoder
      .fromSchema(JsonUnknownExampleOptional.jsonUnknownExampleOptionalSchema)
      .decode(doc)

    assertEquals(res, Right(expected))
  }

  test(
    "unknown field decoding: no unknown field in payload, optional field with default"
  ) {
    val doc = Document.obj(
      "s" -> Document.fromString("foo"),
      "i" -> Document.fromInt(67)
    )
    val expected = JsonUnknownExampleOptional(
      "foo",
      67,
      Some(Map("default" -> Document.fromBoolean(true)))
    )

    val res = Document.Decoder
      .fromSchema(
        JsonUnknownExampleOptionalWithDefault.jsonUnknownExampleOptionalSchema
      )
      .decode(doc)

    assertEquals(res, Right(expected))
  }

  test("unknown field decoding: with unknown fields in payload") {
    val doc = Document.obj(
      "s" -> Document.fromString("foo"),
      "i" -> Document.fromInt(67),
      "someField" -> Document.obj("a" -> Document.fromString("b")),
      "someOtherField" -> Document.fromInt(75)
    )
    val expected = JsonUnknownExample(
      "foo",
      67,
      Map(
        "someField" -> Document.obj("a" -> Document.fromString("b")),
        "someOtherField" -> Document.fromInt(75)
      )
    )

    val res = Document.Decoder
      .fromSchema(JsonUnknownExample.jsonUnknownExampleSchema)
      .decode(doc)

    assertEquals(res, Right(expected))
  }

  test(
    "unknown field decoding: with unknown fields in payload, optional field"
  ) {
    val doc = Document.obj(
      "s" -> Document.fromString("foo"),
      "i" -> Document.fromInt(67),
      "someField" -> Document.obj("a" -> Document.fromString("b")),
      "someOtherField" -> Document.fromInt(75)
    )
    val expected = JsonUnknownExampleOptional(
      "foo",
      67,
      Some(
        Map(
          "someField" -> Document.obj("a" -> Document.fromString("b")),
          "someOtherField" -> Document.fromInt(75)
        )
      )
    )

    val res = Document.Decoder
      .fromSchema(JsonUnknownExampleOptional.jsonUnknownExampleOptionalSchema)
      .decode(doc)

    assertEquals(res, Right(expected))
  }

  test("unknown field decoding: with unknow field explicitely set in payload") {
    val doc = Document.obj(
      "s" -> Document.fromString("foo"),
      "i" -> Document.fromInt(67),
      "someField" -> Document.obj("a" -> Document.fromString("b")),
      "someOtherField" -> Document.fromInt(75),
      "others" -> Document.obj()
    )
    val expected = JsonUnknownExample(
      "foo",
      67,
      Map(
        "someField" -> Document.obj("a" -> Document.fromString("b")),
        "someOtherField" -> Document.fromInt(75),
        "others" -> Document.obj()
      )
    )

    val res = Document.Decoder
      .fromSchema(JsonUnknownExample.jsonUnknownExampleSchema)
      .decode(doc)

    assertEquals(res, Right(expected))
  }

  test("unknown field encoding") {
    val in = JsonUnknownExample(
      "foo",
      67,
      Map(
        "someField" -> Document.obj("a" -> Document.fromString("b")),
        "someOtherField" -> Document.fromInt(75),
        "others" -> Document.obj()
      )
    )

    val expected = Document.obj(
      "s" -> Document.fromString("foo"),
      "i" -> Document.fromInt(67),
      "someField" -> Document.obj("a" -> Document.fromString("b")),
      "someOtherField" -> Document.fromInt(75),
      "others" -> Document.obj()
    )

    val doc = Document.Encoder
      .fromSchema(JsonUnknownExample.jsonUnknownExampleSchema)
      .encode(in)

    assertEquals(doc, expected)
  }

  private def inside[A, B](
      a: A
  )(assertPF: PartialFunction[A, Unit])(implicit loc: munit.Location) = {
    assertPF.lift
      .apply(a)
      .getOrElse(Assertions.fail("Value did not match the expected pattern"))
  }

}
