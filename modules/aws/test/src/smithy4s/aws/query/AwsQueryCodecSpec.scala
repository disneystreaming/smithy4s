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
import smithy4s.ShapeId
import smithy4s.schema.Schema
import smithy4s.schema.CompilationCache
import smithy4s.schema.Schema._
import weaver._
import smithy.api.XmlFlattened
import smithy.api.XmlName

object AwsQueryCodecSpec extends SimpleIOSuite {

  implicit class SchemaOps[A](schema: Schema[A]) {
    def named(name: String) = schema.withId(ShapeId("default", name))
    def x = named("x")
    def n = named("Foo")
  }

  test("primitive: int") {
    implicit val schema: Schema[Int] = int.x
    val xml = "1"
    checkContent(xml, 1)
  }

  test("primitive: string") {
    implicit val schema: Schema[String] = string.x
    val xml = "foo"
    checkContent(xml, "foo")
  }

  test("primitive: boolean") {
    implicit val schema: Schema[Boolean] = boolean.x
    val xml = "true"
    checkContent(xml, true)
  }

  test("primitive: long") {
    implicit val schema: Schema[Long] = long.x
    val xml = "1"
    checkContent(xml, 1L)
  }

  test("primitive: short") {
    implicit val schema: Schema[Short] = short.x
    val xml = "1"
    checkContent(xml, 1.toShort)
  }

  test("primitive: byte") {
    implicit val schema: Schema[Byte] = byte.x
    val xml = "99"
    checkContent(xml, 'c'.toByte)
  }

  test("primitive: double") {
    implicit val schema: Schema[Double] = double.x
    val xml = "1.1"
    checkContent(xml, 1.1)
  }

  test("primitive: float") {
    implicit val schema: Schema[Float] = float.x
    if (!Platform.isJS) {
      val xml = "1.1"
      checkContent(xml, 1.1f)
    } else {
      // 1.1f prints 1.100000023841858 in JS
      val xml = "1"
      checkContent(xml, 1.0f)
    }
  }

  test("primitive: bigint") {
    implicit val schema: Schema[BigInt] = bigint.x
    val xml =
      "1000000000000000000000000000000000000000000000000000000000000000"
    checkContent(
      xml,
      BigInt("1000000000000000000000000000000000000000000000000000000000000000")
    )
  }

  test("primitive: bigdecimal") {
    implicit val schema: Schema[BigDecimal] = bigdecimal.x
    val xml =
      "1000000000000000000000000000000000000000000000000000000000000000.1"
    checkContent(
      xml,
      BigDecimal(
        "1000000000000000000000000000000000000000000000000000000000000000.1"
      )
    )
  }

  test("primitive: bytes") {
    implicit val schema: Schema[ByteArray] = bytes.x
    val xml = "Zm9vYmFy"
    checkContent(xml, ByteArray("foobar".getBytes()))
  }

  test("struct") {
    case class Foo(x: String, y: Option[String])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val x = string.required[Foo]("x", _.x)
        val y = string.optional[Foo]("y", _.y)
        struct(x, y)(Foo.apply).n
      }
    }

    val xml = "x=value-x&y=value-y".stripMargin

    checkContent(xml, Foo("value-x", Some("value-y")))
  }

  test("struct: empty optional") {
    case class Foo(x: String, y: Option[String])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val x = string.required[Foo]("x", _.x)
        val y = string.optional[Foo]("y", _.y)
        struct(x, y)(Foo.apply).n
      }
    }

    val xml = "x=value-x".stripMargin

    checkContent(xml, Foo("value-x", None))
  }

  test("struct: custom names") {
    case class Foo(x: String, y: Option[String])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val x = string.required[Foo]("x", _.x).addHints(XmlName("xx"))
        val y = string.optional[Foo]("y", _.y).addHints(XmlName("y:y"))
        struct(x, y)(Foo.apply).n
      }
    }

    val xml = "xx=value-x&y:y=value-y"

    checkContent(xml, Foo("value-x", Some("value-y")))
  }

  test("list") {
    case class Foo(foos: List[Int])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val foos = list(int)
          .required[Foo]("foos", _.foos)
        struct(foos)(Foo.apply).n
      }
    }

    val xml = "foos.member.1=1&foos.member.2=2&foos.member.3=3"

    checkContent(xml, Foo(List(1, 2, 3)))
  }

  test("list: custom names") {
    case class Foo(foos: List[Int])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val foos = list(int.addHints(XmlName("x")))
          .required[Foo]("foos", _.foos)
        struct(foos)(Foo.apply).n
      }
    }

    val xml = "foos.x.1=1&foos.x.2=2&foos.x.3=3"

    checkContent(xml, Foo(List(1, 2, 3)))
  }

  test("list: flattened") {
    case class Foo(foos: List[Int])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val foos = list(int)
          .required[Foo]("foos", _.foos)
          .addHints(XmlFlattened())
        struct(foos)(Foo.apply).n
      }
    }
    val xml = "foos.1=1&foos.2=2&foos.3=3"
    checkContent(xml, Foo(List(1, 2, 3)))
  }

  test("list: flattened custom names") {
    case class Foo(foos: List[Int])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val foos = list(int)
          .required[Foo]("foos", _.foos)
          .addHints(XmlFlattened(), XmlName("x"))
        struct(foos)(Foo.apply).n
      }
    }
    val xml = "x.1=1&x.2=2&x.3=3"

    checkContent(xml, Foo(List(1, 2, 3)))
  }

  test("recursion") {
    case class Foo(foo: Option[Foo])
    object Foo {
      implicit val schema: Schema[Foo] = recursive {
        val foos = schema.optional[Foo]("foo", _.foo)
        struct(foos)(Foo.apply).n
      }
    }

    val xml = """|<Foo>
                 |   <foo>
                 |      <foo>
                 |      </foo>
                 |   </foo>
                 |</Foo>
                 |""".stripMargin
    checkContent(xml, Foo(Some(Foo(Some(Foo(None))))))
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
    val xmlLeft = "left=1"
    val xmlRight = "right=hello"
    checkContent[Foo](xmlLeft, Left(1)) |+|
      checkContent[Foo](xmlRight, Right("hello"))
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
    val xmlLeft = "foo=1"
    val xmlRight = "bar=hello".stripMargin
    checkContent[Foo](xmlLeft, Left(1)) |+|
      checkContent[Foo](xmlRight, Right("hello"))
  }

  test("enumeration") {
    sealed abstract class FooBar(val stringValue: String, val intValue: Int)
        extends smithy4s.Enumeration.Value {
      val name = stringValue
      val value = stringValue
      val hints = Hints.empty
    }
    object FooBar {
      case object Foo extends FooBar("foo", 0)
      case object Bar extends FooBar("bar", 1)
      implicit val schema: Schema[FooBar] =
        enumeration[FooBar](List(Foo, Bar)).x
    }
    val xmlFoo = "x=foo"
    val xmlBar = "x=bar"
    checkContent[FooBar](xmlFoo, FooBar.Foo) <+>
      checkContent[FooBar](xmlBar, FooBar.Bar)
  }

  test("map") {
    case class Foo(foos: Map[String, Int])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val foos = map(string, int)
          .required[Foo]("foos", _.foos)

        struct(foos)(Foo.apply).n
      }
    }

    val xml =
      "foo.entry.1.key=a&foo.entry.1.value=1&foo.entry.2.key=b&foo.entry.2.value=2"

    checkContent(xml, Foo(Map("a" -> 1, "b" -> 2)))
  }

  test("map: custom names") {
    case class Foo(foos: Map[String, Int])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val foos =
          map(string.addHints(XmlName("k")), int.addHints(XmlName("v")))
            .required[Foo]("foos", _.foos)
            .addHints(XmlName("entries"))
        struct(foos)(Foo.apply).n
      }
    }
    val xml =
      "entries.entry.1.k=a&entries.entry.1.v=1&entries.entry.2.k=b&entries.entry.2.v=2"

    checkContent(xml, Foo(Map("a" -> 1, "b" -> 2)))
  }

  test("map: flattened") {
    case class Foo(foos: Map[String, Int])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val foos =
          map(string, int)
            .required[Foo]("foos", _.foos)
            .addHints(XmlFlattened())
        struct(foos)(Foo.apply).n
      }
    }
    val xml =
      "foos.1.key=a&foos.1.value=1&foos.2.key=b&foos.2.value=2"

    checkContent(xml, Foo(Map("a" -> 1, "b" -> 2)))
  }

  def checkContent[A: Schema](expected: String, value: A)(implicit
      loc: SourceLocation
  ): IO[Expectations] = {
    val schema = implicitly[Schema[A]]
    val cache = CompilationCache.make[AwsQueryCodec]
    val codec = new AwsSchemaVisitorAwsQueryCodec(cache)
    IO(expect.same(codec(schema)(value).render.getOrElse(""), expected))
  }
}
