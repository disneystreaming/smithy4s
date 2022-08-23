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

package smithy4s

import smithy.api.JsonName
import smithy.api.Default
import smithy4s.api.Discriminated
import smithy4s.example.IntList
import munit._

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
  implicit val tupleIntStringSchema: Schema[(Int, String)] = {
    val i = int.required[(Int, String)]("int", _._1)
    val s =
      string
        .required[(Int, String)]("string", _._2)
        .addHints(JsonName("_string"))
    struct(i, s)((_, _))
  }

  implicit val eitherIntStringSchema: Schema[Either[Int, String]] = {
    val left = int.oneOf[Either[Int, String]]("int", (int: Int) => Left(int))
    val right =
      string
        .oneOf[Either[Int, String]]("string", (str: String) => Right(str))
        .addHints(JsonName("_string"))
    union(left, right) {
      case Left(i)    => left(i)
      case Right(str) => right(str)
    }
  }

  case class Foo(str: String)
  case class Bar(str: String, int: Int)

  implicit val eitherFooBarSchema: Schema[Either[Foo, Bar]] = {
    val left = struct(string.required[Foo]("str", _.str))(Foo.apply)
      .oneOf[Either[Foo, Bar]]("foo", (f: Foo) => Left(f))

    val right = struct(
      string.required[Bar]("str", _.str).addHints(JsonName("barStr")),
      int.required[Bar]("int", _.int)
    )(Bar.apply)
      .oneOf[Either[Foo, Bar]]("bar", (b: Bar) => Right(b))
      .addHints(JsonName("barBar"))

    union(left, right) {
      case Left(f)  => left(f)
      case Right(b) => right(b)
    }.addHints(
      Discriminated("type")
    )
  }

  case class Baz()

  implicit val eitherFooBazSchema: Schema[Either[Foo, Baz]] = {
    val left = struct(string.required[Foo]("str", _.str))(Foo.apply)
      .oneOf[Either[Foo, Baz]]("foo", (f: Foo) => Left(f))

    val right = constant(Baz())
      .oneOf[Either[Foo, Baz]]("baz", (b: Baz) => Right(b))

    union(left, right) {
      case Left(f)  => left(f)
      case Right(b) => right(b)
    }.addHints(
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

    expect(document == expectedDocument)
    expect(roundTripped == Right(intAndString))
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
        "type" -> fromString("baz")
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

    expect(fromEmpty == Right(expectedDecoded))
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

    expect(document == expectedDocument)
    expect(roundTripped == Right(defTest))
  }

}
