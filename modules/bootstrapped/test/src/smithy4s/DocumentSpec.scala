/*
 *  Copyright 2021-2023 Disney Streaming
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
import alloy.Discriminated
import munit._
import smithy4s.example.IntList
import smithy4s.example.OperationOutput
import smithy4s.example.DefaultRequiredUnknownFieldRetentionExample
import smithy4s.example.DefaultUnknownFieldRetentionExample
import smithy4s.example.RequiredUnknownFieldRetentionExample
import smithy4s.example.UnknownFieldRetentionExample

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
      .fromSchema(OperationOutput.schema)
      .encode(OperationOutput())

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
        OperationOutput.schema
      )
      .encode(OperationOutput())
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
        OperationOutput.schema
      )
      .encode(OperationOutput())
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
      .fromSchema(OperationOutput.schema)
      .encode(
        OperationOutput(
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
      .fromSchema(OperationOutput.schema)
      .encode(
        OperationOutput(
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

  test(
    "document codec - unknown field retention + explicit defaults encoding = false"
  ) {
    val unknownFieldRetentionExample = UnknownFieldRetentionExample(
      foo = Some("foo"),
      bar = Some("bar"),
      retainedUnknownFields = Some(
        Document.obj(
          "unknownField1" -> Document.fromString("unknownString1"),
          "unknownField2" -> Document.fromString("unknownString2")
        )
      )
    )

    val document = Document.Encoder
      .withExplicitDefaultsEncoding(false)
      .fromSchema(UnknownFieldRetentionExample.schema)
      .encode(unknownFieldRetentionExample)
    import Document._
    val expectedDocument = Document.obj(
      "foo" -> Document.fromString("foo"),
      "bar" -> Document.fromString("bar"),
      "unknownField1" -> Document.fromString("unknownString1"),
      "unknownField2" -> Document.fromString("unknownString2")
    )

    val roundTripped = Document.decode[UnknownFieldRetentionExample](document)

    expect.same(document, expectedDocument)
    expect.same(roundTripped, Right(unknownFieldRetentionExample))
  }
  test(
    "document codec - unknown field retention + explicit defaults encoding = true"
  ) {
    val defaultUnknownFieldRetentionExample =
      DefaultUnknownFieldRetentionExample(
        foo = Some("foo"),
        bar = Some("bar"),
        retainedUnknownFields = Document.obj(
          "unknownField1" -> Document.fromString("unknownString1"),
          "unknownField2" -> Document.fromString("unknownString2")
        )
      )

    val document = Document.Encoder
      .withExplicitDefaultsEncoding(true)
      .fromSchema(DefaultUnknownFieldRetentionExample.schema)
      .encode(defaultUnknownFieldRetentionExample)
    import Document._
    val expectedDocument = Document.obj(
      "foo" -> Document.fromString("foo"),
      "bar" -> Document.fromString("bar"),
      "unknownField1" -> Document.fromString("unknownString1"),
      "unknownField2" -> Document.fromString("unknownString2")
    )

    val roundTripped =
      Document.decode[DefaultUnknownFieldRetentionExample](document)

    expect.same(document, expectedDocument)
    expect.same(roundTripped, Right(defaultUnknownFieldRetentionExample))
  }

  test(
    "document codec - required unknown field retention + explicit defaults encoding = false"
  ) {
    val requiredUnknownFieldRetentionExample =
      RequiredUnknownFieldRetentionExample(
        foo = Some("foo"),
        bar = Some("bar"),
        retainedUnknownFields = Document.obj(
          "unknownField1" -> Document.fromString("unknownString1"),
          "unknownField2" -> Document.fromString("unknownString2")
        )
      )

    val document = Document.Encoder
      .withExplicitDefaultsEncoding(false)
      .fromSchema(RequiredUnknownFieldRetentionExample.schema)
      .encode(requiredUnknownFieldRetentionExample)
    import Document._
    val expectedDocument = Document.obj(
      "foo" -> Document.fromString("foo"),
      "bar" -> Document.fromString("bar"),
      "unknownField1" -> Document.fromString("unknownString1"),
      "unknownField2" -> Document.fromString("unknownString2")
    )

    val roundTripped =
      Document.decode[RequiredUnknownFieldRetentionExample](document)

    expect.same(document, expectedDocument)
    expect.same(roundTripped, Right(requiredUnknownFieldRetentionExample))
  }

  test(
    "document codec - required unknown field retention + explicit defaults encoding = true"
  ) {
    val defaultRequiredUnknownFieldRetentionExample =
      DefaultRequiredUnknownFieldRetentionExample(
        foo = Some("foo"),
        bar = Some("bar"),
        retainedUnknownFields = Document.obj(
          "unknownField1" -> Document.fromString("unknownString1"),
          "unknownField2" -> Document.fromString("unknownString2")
        )
      )

    val document = Document.Encoder
      .withExplicitDefaultsEncoding(true)
      .fromSchema(DefaultRequiredUnknownFieldRetentionExample.schema)
      .encode(defaultRequiredUnknownFieldRetentionExample)
    import Document._
    val expectedDocument = Document.obj(
      "foo" -> Document.fromString("foo"),
      "bar" -> Document.fromString("bar"),
      "unknownField1" -> Document.fromString("unknownString1"),
      "unknownField2" -> Document.fromString("unknownString2")
    )

    val roundTripped =
      Document.decode[DefaultRequiredUnknownFieldRetentionExample](document)

    expect.same(document, expectedDocument)
    expect.same(
      roundTripped,
      Right(defaultRequiredUnknownFieldRetentionExample)
    )
  }

}
