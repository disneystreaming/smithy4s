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

package smithy4s.aws.query

import cats.effect.IO
import cats.syntax.all._
import smithy4s.ByteArray
import smithy4s.Hints
import smithy4s.schema.Schema
import smithy4s.schema.CompilationCache
import smithy4s.schema.Schema._
import weaver._
import smithy.api.XmlFlattened
import smithy.api.XmlName

object AwsQueryCodecSpec extends SimpleIOSuite {

  test("primitive: int") {
    implicit val schema: Schema[Int] = int
    val expected = "1"
    checkContent(expected, 1)
  }

  test("primitive: string") {
    implicit val schema: Schema[String] = string
    val expected = "foo"
    checkContent(expected, "foo")
  }

  test("primitive: boolean") {
    implicit val schema: Schema[Boolean] = boolean
    val expected = "true"
    checkContent(expected, true)
  }

  test("primitive: long") {
    implicit val schema: Schema[Long] = long
    val expected = "1"
    checkContent(expected, 1L)
  }

  test("primitive: short") {
    implicit val schema: Schema[Short] = short
    val expected = "1"
    checkContent(expected, 1.toShort)
  }

  test("primitive: byte") {
    implicit val schema: Schema[Byte] = byte
    val expected = "99"
    checkContent(expected, 'c'.toByte)
  }

  test("primitive: double") {
    implicit val schema: Schema[Double] = double
    val expected = "1.1"
    checkContent(expected, 1.1)
  }

  test("primitive: float") {
    implicit val schema: Schema[Float] = float
    if (!Platform.isJS) {
      val expected = "1.1"
      checkContent(expected, 1.1f)
    } else {
      // 1.1f prints 1.100000023841858 in JS
      val expected = "1"
      checkContent(expected, 1.0f)
    }
  }

  test("primitive: bigint") {
    implicit val schema: Schema[BigInt] = bigint
    val expected =
      "1000000000000000000000000000000000000000000000000000000000000000"
    checkContent(
      expected,
      BigInt("1000000000000000000000000000000000000000000000000000000000000000")
    )
  }

  test("primitive: bigdecimal") {
    implicit val schema: Schema[BigDecimal] = bigdecimal
    val expected =
      "1000000000000000000000000000000000000000000000000000000000000000.1"
    checkContent(
      expected,
      BigDecimal(
        "1000000000000000000000000000000000000000000000000000000000000000.1"
      )
    )
  }

  test("primitive: bytes") {
    implicit val schema: Schema[ByteArray] = bytes
    val expected = "Zm9vYmFy"
    checkContent(expected, ByteArray("foobar".getBytes()))
  }

  test("struct") {
    case class Foo(x: String, y: Option[String])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val x = string.required[Foo]("x", _.x)
        val y = string.optional[Foo]("y", _.y)
        struct(x, y)(Foo.apply)
      }
    }

    val expected = "x=value-x&y=value-y"

    checkContent(expected, Foo("value-x", Some("value-y")))
  }

  test("struct: empty optional") {
    case class Foo(x: String, y: Option[String])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val x = string.required[Foo]("x", _.x)
        val y = string.optional[Foo]("y", _.y)
        struct(x, y)(Foo.apply)
      }
    }

    val expected = "x=value-x"

    checkContent(expected, Foo("value-x", None))
  }

  test("struct: custom names") {
    case class Foo(x: String, y: Option[String])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val x = string.required[Foo]("x", _.x).addHints(XmlName("xx"))
        val y = string.optional[Foo]("y", _.y).addHints(XmlName("y:y"))
        struct(x, y)(Foo.apply)
      }
    }

    val expected = "xx=value-x&y%3Ay=value-y"

    checkContent(expected, Foo("value-x", Some("value-y")))
  }

  test("list") {
    case class Foo(foos: List[Int])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val foos = list(int).required[Foo]("foos", _.foos)
        struct(foos)(Foo.apply)
      }
    }

    val expected = "foos.member.1=1&foos.member.2=2&foos.member.3=3"

    checkContent(expected, Foo(List(1, 2, 3)))
  }

  test("list: custom names") {
    case class Foo(foos: List[Int])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val foos = list(int.addHints(XmlName("x")))
          .required[Foo]("foos", _.foos)
        struct(foos)(Foo.apply)
      }
    }

    val expected = "foos.x.1=1&foos.x.2=2&foos.x.3=3"

    checkContent(expected, Foo(List(1, 2, 3)))
  }

  test("list: flattened") {
    case class Foo(foos: List[Int])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val foos = list(int)
          .required[Foo]("foos", _.foos)
          .addHints(XmlFlattened())
        struct(foos)(Foo.apply)
      }
    }
    val expected = "foos.1=1&foos.2=2&foos.3=3"
    checkContent(expected, Foo(List(1, 2, 3)))
  }

  test("list: flattened custom names") {
    case class Foo(foos: List[Int])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val foos = list(int)
          .required[Foo]("foos", _.foos)
          .addHints(XmlFlattened(), XmlName("x"))
        struct(foos)(Foo.apply)
      }
    }
    val expected = "x.1=1&x.2=2&x.3=3"

    checkContent(expected, Foo(List(1, 2, 3)))
  }

  test("recursion") {
    case class IntList(head: Int, tail: Option[IntList])
    object IntList {
      implicit val schema: Schema[IntList] = recursive {
        val head = int.required[IntList]("head", _.head)
        val tail = schema.optional[IntList]("tail", _.tail)
        struct(head, tail)(IntList.apply)
      }
    }

    val input = IntList(1, Some(IntList(2, None)))

    val expected = "head=1&tail.head=2"
    checkContent(expected, input)
  }

  test("union") {
    type Foo = Either[Int, String]
    implicit val schema: Schema[Foo] = {
      val left = int.oneOf[Foo]("left", Left(_))
      val right = string.oneOf[Foo]("right", Right(_))
      union(left, right) {
        case Left(int)     => left(int)
        case Right(string) => right(string)
      }
    }
    val expectedLeft = "left=1"
    val expectedRight = "right=hello"
    checkContent[Foo](expectedLeft, Left(1)) |+|
      checkContent[Foo](expectedRight, Right("hello"))
  }

  test("union: custom names") {
    type Foo = Either[Int, String]
    implicit val schema: Schema[Foo] = {
      val left = int.oneOf[Foo]("left", Left(_)).addHints(XmlName("foo"))
      val right = string.oneOf[Foo]("right", Right(_)).addHints(XmlName("bar"))
      union(left, right) {
        case Left(int)     => left(int)
        case Right(string) => right(string)
      }
    }
    val expectedLeft = "foo=1"
    val expectedRight = "bar=hello".stripMargin
    checkContent[Foo](expectedLeft, Left(1)) |+|
      checkContent[Foo](expectedRight, Right("hello"))
  }

  test("enumeration") {
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
    val expectedFoo = "foo"
    val expectedBar = "bar"
    checkContent[FooBar](expectedFoo, FooBar.Foo) <+>
      checkContent[FooBar](expectedBar, FooBar.Bar)
  }

  test("map") {
    case class Foo(foos: Map[String, Int])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val foos = map(string, int).required[Foo]("foos", _.foos)
        struct(foos)(Foo.apply)
      }
    }

    val expected =
      "foos.entry.1.key=a&foos.entry.1.value=1&foos.entry.2.key=b&foos.entry.2.value=2"

    checkContent(expected, Foo(Map("a" -> 1, "b" -> 2)))
  }

  test("map: custom names") {
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
    val expected =
      "entries.entry.1.k=a&entries.entry.1.v=1&entries.entry.2.k=b&entries.entry.2.v=2"

    checkContent(expected, Foo(Map("a" -> 1, "b" -> 2)))
  }

  test("map: flattened") {
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
    val expected =
      "foos.1.key=a&foos.1.value=1&foos.2.key=b&foos.2.value=2"

    checkContent(expected, Foo(Map("a" -> 1, "b" -> 2)))
  }

  def checkContent[A](expected: String, value: A)(implicit
      schema: Schema[A],
      loc: SourceLocation
  ): IO[Expectations] = {
    val cache: CompilationCache[AwsQueryCodec] =
      CompilationCache.make[AwsQueryCodec]
    val schemaVisitor: AwsSchemaVisitorAwsQueryCodec =
      new AwsSchemaVisitorAwsQueryCodec(cache)
    val codec: AwsQueryCodec[A] = schemaVisitor(schema)
    val formData: FormData = codec(value)
    val result: String = formData.render
    IO(expect.same(result, expected))
  }
}
