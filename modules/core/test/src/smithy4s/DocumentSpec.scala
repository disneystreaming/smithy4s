/*
 *  Copyright 2021 Disney Streaming
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
import smithy4s.api.Discriminated
import smithy4s.example.IntList
import weaver._

object DocumentSpec extends FunSuite {

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

    expect(document == expectedDocument) &&
    expect(roundTripped == Right(recursive))
  }

  import smithy4s.syntax._
  implicit val tupleIntStringSchema: Static[Schema[(Int, String)]] =
    Static {
      val i = int.required[(Int, String)]("int", _._1)
      val s =
        string
          .required[(Int, String)]("string", _._2)
          .withHints(JsonName("_string"))
      struct(i, s)((_, _))
    }

  implicit val eitherIntStringSchema: Static[Schema[Either[Int, String]]] =
    Static {
      val left = int.oneOf[Either[Int, String]]("int", (int: Int) => Left(int))
      val right =
        string
          .oneOf[Either[Int, String]]("string", (str: String) => Right(str))
          .withHints(JsonName("_string"))
      union(left, right) {
        case Left(i)    => left(i)
        case Right(str) => right(str)
      }
    }

  case class Foo(str: String)
  case class Bar(str: String, int: Int)

  implicit val eitherFooBarSchema: Static[Schema[Either[Foo, Bar]]] =
    Static {
      val left = struct(string.required[Foo]("str", _.str))(Foo.apply)
        .oneOf[Either[Foo, Bar]]("foo", (f: Foo) => Left(f))

      val right = struct(
        string.required[Bar]("str", _.str).withHints(JsonName("barStr")),
        int.required[Bar]("int", _.int)
      )(Bar.apply)
        .oneOf[Either[Foo, Bar]]("bar", (b: Bar) => Right(b))
        .withHints(JsonName("barBar"))

      union(left, right) {
        case Left(f)  => left(f)
        case Right(b) => right(b)
      }.withHints(
        Discriminated("type")
      )
    }

  case class Baz()

  implicit val eitherFooBazSchema: Static[Schema[Either[Foo, Baz]]] =
    Static {
      val left = struct(string.required[Foo]("str", _.str))(Foo.apply)
        .oneOf[Either[Foo, Baz]]("foo", (f: Foo) => Left(f))

      val right = genericStruct(Vector.empty)(_ => Baz())
        .oneOf[Either[Foo, Baz]]("baz", (b: Baz) => Right(b))

      union(left, right) {
        case Left(f)  => left(f)
        case Right(b) => right(b)
      }.withHints(
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

    expect(document == expectedDocument) &&
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

    expect(document == expectedDocument) &&
    expect(roundTripped == Right(intOrString))
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

    expect(document == expectedDocument) &&
    expect(roundTripped == Right(fooOrBar))
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

    expect(document == expectedDocument) &&
    expect(roundTripped == Right(fooOrBaz))
  }

}
