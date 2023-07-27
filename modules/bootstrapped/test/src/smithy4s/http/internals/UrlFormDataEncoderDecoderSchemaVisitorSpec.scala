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
package http
package internals

import cats.effect.IO
import cats.syntax.all._
import smithy.api.XmlFlattened
import smithy.api.XmlName
import smithy4s.Blob
import smithy4s.Hints
import smithy4s.schema.CompilationCache
import smithy4s.schema.Schema
import smithy4s.schema.Schema._
import weaver._

import scala.collection.mutable

object UrlFormDataEncoderDecoderSchemaVisitorSpec extends SimpleIOSuite {

  test("primitive encoding: int") {
    implicit val schema: Schema[Int] = int
    checkEncoding(
      value = 1,
      rendered = "1"
    )
  }

  test("primitive encoding: string") {
    implicit val schema: Schema[String] = string
    checkEncoding(
      value = "foo",
      rendered = "foo"
    )
  }

  test("primitive encoding: boolean") {
    implicit val schema: Schema[Boolean] = boolean
    checkEncoding(
      value = true,
      rendered = "true"
    )
  }

  test("primitive encoding: long") {
    implicit val schema: Schema[Long] = long
    checkEncoding(
      value = 1L,
      rendered = "1"
    )
  }

  test("primitive encoding: short") {
    implicit val schema: Schema[Short] = short
    checkEncoding(
      value = 1.toShort,
      rendered = "1"
    )
  }

  test("primitive encoding: byte") {
    implicit val schema: Schema[Byte] = byte
    checkEncoding(
      value = 'c'.toByte,
      rendered = "99"
    )
  }

  test("primitive encoding: double") {
    implicit val schema: Schema[Double] = double
    checkEncoding(
      value = 1.1,
      rendered = "1.1"
    )
  }

  test("primitive encoding: float") {
    implicit val schema: Schema[Float] = float
    if (!Platform.isJS) {
      checkEncoding(
        value = 1.1f,
        rendered = "1.1"
      )
    } else {
      // 1.1f prints 1.100000023841858 in JS
      checkEncoding(
        value = 1.0f,
        rendered = "1"
      )
    }
  }

  test("primitive encoding: bigint") {
    implicit val schema: Schema[BigInt] = bigint
    checkEncoding(
      value = BigInt(
        "1000000000000000000000000000000000000000000000000000000000000000"
      ),
      rendered =
        "1000000000000000000000000000000000000000000000000000000000000000"
    )
  }

  test("primitive encoding: bigdecimal") {
    implicit val schema: Schema[BigDecimal] = bigdecimal
    checkEncoding(
      value = BigDecimal(
        "1000000000000000000000000000000000000000000000000000000000000000.1"
      ),
      rendered =
        "1000000000000000000000000000000000000000000000000000000000000000.1"
    )
  }

  test("primitive encoding: bytes") {
    implicit val schema: Schema[Blob] = bytes
    checkEncoding(
      value = Blob("foobar"),
      rendered = "Zm9vYmFy"
    )
  }

  test("struct encoding and decoding") {
    case class Foo(x: String, y: Option[String])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val x = string.required[Foo]("x", _.x)
        val y = string.optional[Foo]("y", _.y)
        struct(x, y)(Foo.apply)
      }
    }
    checkBoth(
      value = Foo("value-x", Some("value-y")),
      rendered = "x=value-x&y=value-y"
    )
  }

  test("struct encoding and decoding: empty optional") {
    case class Foo(x: String, y: Option[String])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val x = string.required[Foo]("x", _.x)
        val y = string.optional[Foo]("y", _.y)
        struct(x, y)(Foo.apply)
      }
    }
    checkBoth(
      value = Foo("value-x", None),
      rendered = "x=value-x"
    )
  }

  test("struct encoding and decoding: custom names") {
    case class Foo(x: String, y: Option[String])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val x = string.required[Foo]("x", _.x).addHints(XmlName("xx"))
        val y = string.optional[Foo]("y", _.y).addHints(XmlName("y:y"))
        struct(x, y)(Foo.apply)
      }
    }
    checkBoth(
      value = Foo("value-x", Some("value-y")),
      rendered = "xx=value-x&y%3Ay=value-y"
    )
  }

  test("list encoding and decoding") {
    case class Foo(foos: List[Int])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val foos = list(int).required[Foo]("foos", _.foos)
        struct(foos)(Foo.apply)
      }
    }
    checkBoth(
      value = Foo(List(1, 2, 3)),
      rendered = "foos.member.1=1&foos.member.2=2&foos.member.3=3"
    )
  }

  test("list encoding and decoding: custom names") {
    case class Foo(foos: List[Int])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val foos = list(int.addHints(XmlName("x")))
          .required[Foo]("foos", _.foos)
        struct(foos)(Foo.apply)
      }
    }
    checkBoth(
      value = Foo(List(1, 2, 3)),
      rendered = "foos.x.1=1&foos.x.2=2&foos.x.3=3"
    )
  }

  test("list encoding and decoding: flattened") {
    case class Foo(foos: List[Int])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val foos = list(int)
          .required[Foo]("foos", _.foos)
          .addHints(XmlFlattened())
        struct(foos)(Foo.apply)
      }
    }
    checkBoth(
      value = Foo(List(1, 2, 3)),
      rendered = "foos.1=1&foos.2=2&foos.3=3"
    )
  }

  test("list encoding and decoding: flattened custom names") {
    case class Foo(foos: List[Int])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val foos = list(int)
          .required[Foo]("foos", _.foos)
          .addHints(XmlFlattened(), XmlName("x"))
        struct(foos)(Foo.apply)
      }
    }
    checkBoth(
      value = Foo(List(1, 2, 3)),
      rendered = "x.1=1&x.2=2&x.3=3"
    )
  }

  test("recursion encoding and decoding") {
    case class IntList(head: Int, tail: Option[IntList])
    object IntList {
      implicit val schema: Schema[IntList] = recursive {
        val head = int.required[IntList]("head", _.head)
        val tail = schema.optional[IntList]("tail", _.tail)
        struct(head, tail)(IntList.apply)
      }
    }
    checkBoth(
      value = IntList(1, Some(IntList(2, None))),
      rendered = "head=1&tail.head=2"
    )
  }

  test("union encoding and decoding") {
    type Foo = Either[Int, String]
    implicit val schema: Schema[Foo] = Schema.either(int, string)
    checkBoth[Foo](
      value = Left(1),
      rendered = "left=1"
    ) |+| checkBoth[Foo](
      value = Right("hello"),
      rendered = "right=hello"
    )
  }

  test("union encoding and decoding: custom names") {
    type Foo = Either[Int, String]
    implicit val schema: Schema[Foo] = Schema.either(
      int.addMemberHints(XmlName("foo")),
      string.addMemberHints(XmlName("bar"))
    )
    checkBoth[Foo](
      value = Left(1),
      rendered = "foo=1"
    ) |+| checkBoth[Foo](
      value = Right("hello"),
      rendered = "bar=hello"
    )
  }

  test("enumeration encoding") {
    sealed abstract class FooBar(val value: String, val intValue: Int)
        extends smithy4s.Enumeration.Value {
      val name = value
      val hints = Hints.empty
      type EnumType = FooBar
      def enumeration: smithy4s.Enumeration[FooBar] = FooBar
    }
    object FooBar extends smithy4s.Enumeration[FooBar] {
      def hints: smithy4s.Hints = Hints.empty
      def id: smithy4s.ShapeId = smithy4s.ShapeId("test", "FooBar")
      case object Foo extends FooBar("foo", 0)
      case object Bar extends FooBar("bar", 1)
      def values = List(Foo, Bar)
      implicit val schema: Schema[FooBar] =
        Schema.stringEnumeration[FooBar](List(Foo, Bar))
    }
    checkEncoding[FooBar](
      value = FooBar.Foo,
      rendered = "foo"
    ) <+> checkEncoding[FooBar](
      value = FooBar.Bar,
      rendered = "bar"
    )
  }

  test("map encoding and decoding") {
    case class Foo(foos: Map[String, Int])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val foos = map(string, int).required[Foo]("foos", _.foos)
        struct(foos)(Foo.apply)
      }
    }
    checkBoth(
      value = Foo(Map("a" -> 1, "b" -> 2)),
      rendered =
        "foos.entry.1.key=a&foos.entry.1.value=1&foos.entry.2.key=b&foos.entry.2.value=2"
    )
  }

  test("map encoding and decoding: custom names") {
    case class Foo(foos: Map[String, Int])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val foos =
          map(string.addHints(XmlName("k")), int.addHints(XmlName("v")))
            .required[Foo]("foos", _.foos)
            .addHints(XmlName("entries"))
        struct(foos)(Foo.apply)
      }
    }
    checkBoth(
      value = Foo(Map("a" -> 1, "b" -> 2)),
      rendered =
        "entries.entry.1.k=a&entries.entry.1.v=1&entries.entry.2.k=b&entries.entry.2.v=2"
    )
  }

  test("map encoding and decoding: flattened") {
    case class Foo(foos: Map[String, Int])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val foos =
          map(string, int)
            .required[Foo]("foos", _.foos)
            .addHints(XmlFlattened())
        struct(foos)(Foo.apply)
      }
    }
    checkBoth(
      value = Foo(Map("a" -> 1, "b" -> 2)),
      rendered = "foos.1.key=a&foos.1.value=1&foos.2.key=b&foos.2.value=2"
    )
  }

  private def checkEncoding[A: Schema](value: A, rendered: String)(implicit
      loc: SourceLocation
  ): IO[Expectations] = {
    val encodedFormData = encodeFormData(value)
    val builder = new mutable.StringBuilder
    encodedFormData.writeTo(builder)
    val encodedString = builder.result()
    IO(expect.same(encodedString, rendered))
  }

  def checkBoth[A](value: A, rendered: String)(implicit
      schema: Schema[A],
      loc: SourceLocation
  ): IO[Expectations] = {
    val encodedUrlForm = encodeUrlForm(value)
    val builder = new mutable.StringBuilder
    encodedUrlForm.formData.writeTo(builder)
    val encodedString = builder.result()
    for {
      decodedResult <- for {
        urlForm <- parseUrlForm(rendered)
        value <- decodeUrlForm(urlForm)
      } yield value
    } yield expect.same(encodedString, rendered) &&
      expect.same(decodedResult, value)
  }

  // private def checkUrlForm[A: Schema](urlFormString: String, expected: A)(
  //     implicit loc: SourceLocation
  // ): IO[Expectations] =
  //   parseUrlForm(urlFormString)
  //     .flatMap(decodeUrlForm[A](_))
  //     .map(result => expect.same(result, expected))

  def decodeUrlForm[A](
      urlForm: UrlForm
  )(implicit schema: Schema[A]): IO[A] =
    UrlForm
      .Decoder(
        ignoreXmlFlattened = false,
        capitalizeStructAndUnionMemberNames = false
      )
      .fromSchema(schema)
      .decode(urlForm)
      .leftWiden[Throwable]
      .liftTo[IO]

  def encodeFormData[A](
      value: A
  )(implicit schema: Schema[A]): UrlForm.FormData = {
    val encoderCache = CompilationCache.make[UrlFormDataEncoder]
    val encoderSchemaVisitor = new UrlFormDataEncoderSchemaVisitor(
      encoderCache,
      ignoreXmlFlattened = false,
      capitalizeStructAndUnionMemberNames = false
    )
    val encoder = encoderSchemaVisitor(schema)
    encoder.encode(value)
  }

  def encodeUrlForm[A](value: A)(implicit schema: Schema[A]): UrlForm =
    UrlForm
      .Encoder(
        ignoreXmlFlattened = false,
        capitalizeStructAndUnionMemberNames = false
      )
      .fromSchema(schema)
      .encode(value)

  def parseUrlForm(urlFormString: String): IO[UrlForm] =
    IO.fromEither(UrlFormParser.parseUrlForm(urlFormString))

}
