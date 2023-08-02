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

import smithy.api.XmlFlattened
import smithy.api.XmlName
import smithy4s.Blob
import smithy4s.Hints
import smithy4s.schema.Schema
import smithy4s.schema.Schema._
import weaver._

object UrlFormDataEncoderDecoderSchemaVisitorSpec extends SimpleIOSuite {

  pureTest("primitive: int") {
    implicit val schema: Schema[Int] = int
    checkEncodingAndDecoding(
      value = 1,
      renderedValue = "=1"
    )
  }

  pureTest("primitive: string") {
    implicit val schema: Schema[String] = string
    checkEncodingAndDecoding(
      value = "foo",
      renderedValue = "=foo"
    )
  }

  pureTest("primitive: boolean") {
    implicit val schema: Schema[Boolean] = boolean
    checkEncodingAndDecoding(
      value = true,
      renderedValue = "=true"
    )
  }

  pureTest("primitive: long") {
    implicit val schema: Schema[Long] = long
    checkEncodingAndDecoding(
      value = 1L,
      renderedValue = "=1"
    )
  }

  pureTest("primitive: short") {
    implicit val schema: Schema[Short] = short
    checkEncodingAndDecoding(
      value = 1.toShort,
      renderedValue = "=1"
    )
  }

  pureTest("primitive: byte") {
    implicit val schema: Schema[Byte] = byte
    checkEncodingAndDecoding(
      value = 'c'.toByte,
      renderedValue = "=99"
    )
  }

  pureTest("primitive: double") {
    implicit val schema: Schema[Double] = double
    checkEncodingAndDecoding(
      value = 1.1,
      renderedValue = "=1.1"
    )
  }

  pureTest("primitive: float") {
    implicit val schema: Schema[Float] = float
    if (!Platform.isJS) {
      checkEncodingAndDecoding(
        value = 1.1f,
        renderedValue = "=1.1"
      )
    } else {
      // 1.1f prints 1.100000023841858 in JS
      checkEncodingAndDecoding(
        value = 1.0f,
        renderedValue = "=1"
      )
    }
  }

  pureTest("primitive: bigint") {
    implicit val schema: Schema[BigInt] = bigint
    checkEncodingAndDecoding(
      value = BigInt(
        "1000000000000000000000000000000000000000000000000000000000000000"
      ),
      renderedValue =
        "=1000000000000000000000000000000000000000000000000000000000000000"
    )
  }

  pureTest("primitive: bigdecimal") {
    implicit val schema: Schema[BigDecimal] = bigdecimal
    checkEncodingAndDecoding(
      value = BigDecimal(
        "1000000000000000000000000000000000000000000000000000000000000000.1"
      ),
      renderedValue =
        "=1000000000000000000000000000000000000000000000000000000000000000.1"
    )
  }

  pureTest("primitive: bytes") {
    implicit val schema: Schema[Blob] = bytes
    checkEncodingAndDecoding(
      value = Blob("foobar"),
      renderedValue = "=Zm9vYmFy"
    )
  }

  pureTest("struct") {
    case class Foo(x: String, y: Option[String])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val x = string.required[Foo]("x", _.x)
        val y = string.optional[Foo]("y", _.y)
        struct(x, y)(Foo.apply)
      }
    }
    checkEncodingAndDecoding(
      value = Foo("value-x", Some("value-y")),
      renderedValue = "x=value-x&y=value-y"
    )
  }

  pureTest("struct: empty optional") {
    case class Foo(x: String, y: Option[String])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val x = string.required[Foo]("x", _.x)
        val y = string.optional[Foo]("y", _.y)
        struct(x, y)(Foo.apply)
      }
    }
    checkEncodingAndDecoding(
      value = Foo("value-x", None),
      renderedValue = "x=value-x"
    )
  }

  pureTest("struct: custom names") {
    case class Foo(x: String, y: Option[String])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val x = string.required[Foo]("x", _.x).addHints(XmlName("xx"))
        val y = string.optional[Foo]("y", _.y).addHints(XmlName("y:y"))
        struct(x, y)(Foo.apply)
      }
    }
    checkEncodingAndDecoding(
      value = Foo("value-x", Some("value-y")),
      renderedValue = "xx=value-x&y%3Ay=value-y"
    )
  }

  pureTest("list") {
    case class Foo(foos: List[Int])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val foos = list(int).required[Foo]("foos", _.foos)
        struct(foos)(Foo.apply)
      }
    }
    checkEncodingAndDecoding(
      value = Foo(List(1, 2, 3)),
      renderedValue = "foos.member.1=1&foos.member.2=2&foos.member.3=3"
    )
  }

  pureTest("list: custom names") {
    case class Foo(foos: List[Int])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val foos = list(int.addHints(XmlName("x")))
          .required[Foo]("foos", _.foos)
        struct(foos)(Foo.apply)
      }
    }
    checkEncodingAndDecoding(
      value = Foo(List(1, 2, 3)),
      renderedValue = "foos.x.1=1&foos.x.2=2&foos.x.3=3"
    )
  }

  pureTest("list: flattened") {
    case class Foo(foos: List[Int])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val foos = list(int)
          .required[Foo]("foos", _.foos)
          .addHints(XmlFlattened())
        struct(foos)(Foo.apply)
      }
    }
    checkEncodingAndDecoding(
      value = Foo(List(1, 2, 3)),
      renderedValue = "foos.1=1&foos.2=2&foos.3=3"
    )
  }

  pureTest("list: flattened custom names") {
    case class Foo(foos: List[Int])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val foos = list(int)
          .required[Foo]("foos", _.foos)
          .addHints(XmlFlattened(), XmlName("x"))
        struct(foos)(Foo.apply)
      }
    }
    checkEncodingAndDecoding(
      value = Foo(List(1, 2, 3)),
      renderedValue = "x.1=1&x.2=2&x.3=3"
    )
  }

  pureTest("recursion") {
    case class IntList(head: Int, tail: Option[IntList])
    object IntList {
      implicit val schema: Schema[IntList] = recursive {
        val head = int.required[IntList]("head", _.head)
        val tail = schema.optional[IntList]("tail", _.tail)
        struct(head, tail)(IntList.apply)
      }
    }
    checkEncodingAndDecoding(
      value = IntList(1, Some(IntList(2, None))),
      renderedValue = "head=1&tail.head=2"
    )
  }

  pureTest("union") {
    type Foo = Either[Int, String]
    implicit val schema: Schema[Foo] = Schema.either(int, string)
    checkEncodingAndDecoding[Foo](
      value = Left(1),
      renderedValue = "left=1"
    ) && checkEncodingAndDecoding[Foo](
      value = Right("hello"),
      renderedValue = "right=hello"
    )
  }

  pureTest("union: custom names") {
    type Foo = Either[Int, String]
    implicit val schema: Schema[Foo] = Schema.either(
      int.addMemberHints(XmlName("foo")),
      string.addMemberHints(XmlName("bar"))
    )
    checkEncodingAndDecoding[Foo](
      value = Left(1),
      renderedValue = "foo=1"
    ) && checkEncodingAndDecoding[Foo](
      value = Right("hello"),
      renderedValue = "bar=hello"
    )
  }

  pureTest("enumeration") {
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
    checkEncodingAndDecoding[FooBar](
      value = FooBar.Foo,
      renderedValue = "=foo"
    ) && checkEncodingAndDecoding[FooBar](
      value = FooBar.Bar,
      renderedValue = "=bar"
    )
  }

  pureTest("map") {
    case class Foo(foos: Map[String, Int])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val foos = map(string, int).required[Foo]("foos", _.foos)
        struct(foos)(Foo.apply)
      }
    }
    checkEncodingAndDecoding(
      value = Foo(Map("a" -> 1, "b" -> 2)),
      renderedValue =
        "foos.entry.1.key=a&foos.entry.1.value=1&foos.entry.2.key=b&foos.entry.2.value=2"
    )
  }

  pureTest("map: custom names") {
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
    checkEncodingAndDecoding(
      value = Foo(Map("a" -> 1, "b" -> 2)),
      renderedValue =
        "entries.entry.1.k=a&entries.entry.1.v=1&entries.entry.2.k=b&entries.entry.2.v=2"
    )
  }

  pureTest("map: flattened") {
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
    checkEncodingAndDecoding(
      value = Foo(Map("a" -> 1, "b" -> 2)),
      renderedValue = "foos.1.key=a&foos.1.value=1&foos.2.key=b&foos.2.value=2"
    )
  }

  private def checkEncodingAndDecoding[A](value: A, renderedValue: String)(
      implicit
      schema: Schema[A],
      loc: SourceLocation
  ): Expectations =
    expect.same(
      UrlForm
        .Encoder(
          ignoreXmlFlattened = false,
          capitalizeStructAndUnionMemberNames = false
        )
        .fromSchema(schema)
        .encode(value)
        .render,
      renderedValue
    ) &&
      expect.same(
        UrlFormParser
          .parse(renderedValue)
          .flatMap(urlForm =>
            UrlForm
              .Decoder(
                ignoreXmlFlattened = false,
                capitalizeStructAndUnionMemberNames = false
              )
              .fromSchema(schema)
              .decode(urlForm)
          ),
        Right(value)
      )

}
