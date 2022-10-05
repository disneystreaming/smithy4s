package smithy4s.xml

import smithy4s.xml.internals.XmlSchemaVisitor
import smithy4s.xml.internals.XmlCursor
import weaver._
import fs2._
import fs2.data.xml._
import fs2.data.xml.dom._
import smithy4s.schema.Schema
import cats.effect.IO
import cats.syntax.all._
import smithy4s.schema.Schema._
import smithy.api.XmlName
import smithy.api.XmlAttribute
import smithy.api.XmlFlattened
import smithy4s.ByteArray

object XmlDecodingSpec extends SimpleIOSuite {

  test("int") {
    implicit val schema: Schema[Int] = int
    val xml = "<x>1</x>"
    testDecode(xml, 1)
  }

  test("string") {
    implicit val schema: Schema[String] = string
    val xml = "<x>foo</x>"
    testDecode(xml, "foo")
  }

  test("boolean") {
    implicit val schema: Schema[Boolean] = boolean
    val xml = "<x>true</x>"
    testDecode(xml, true)
  }

  test("long") {
    implicit val schema: Schema[Long] = long
    val xml = "<x>1</x>"
    testDecode(xml, 1L)
  }

  test("short") {
    implicit val schema: Schema[Short] = short
    val xml = "<x>1</x>"
    testDecode(xml, 1.toShort)
  }

  test("byte") {
    implicit val schema: Schema[Byte] = byte
    val xml = "<x>99</x>"
    testDecode(xml, 'c'.toByte)
  }

  test("double") {
    implicit val schema: Schema[Double] = double
    val xml = "<x>1.1</x>"
    testDecode(xml, 1.1)
  }

  test("float") {
    implicit val schema: Schema[Float] = float
    val xml = "<x>1.1</x>"
    testDecode(xml, 1.1f)
  }

  test("bigint") {
    implicit val schema: Schema[BigInt] = bigint
    val xml =
      "<x>1000000000000000000000000000000000000000000000000000000000000000</x>"
    testDecode(
      xml,
      BigInt("1000000000000000000000000000000000000000000000000000000000000000")
    )
  }

  test("bigdecimal") {
    implicit val schema: Schema[BigDecimal] = bigdecimal
    val xml =
      "<x>1000000000000000000000000000000000000000000000000000000000000000.1</x>"
    testDecode(
      xml,
      BigDecimal(
        "1000000000000000000000000000000000000000000000000000000000000000.1"
      )
    )
  }

  test("bytes") {
    implicit val schema: Schema[ByteArray] = bytes
    val xml = "<x>Zm9vYmFy</x>"
    testDecode(xml, ByteArray("foobar".getBytes()))
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

    val xml = """|<Foo>
                  |  <x>x</x>
                  |  <y>y</y>
                  |</Foo>""".stripMargin

    testDecode(xml, Foo("x", Some("y")))
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

    val xml = """|<Foo>
                  |  <x>x</x>
                  |</Foo>""".stripMargin

    testDecode(xml, Foo("x", None))
  }

  test("struct: custom names") {
    case class Foo(x: String, y: Option[String])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val x = string.required[Foo]("x", _.x).addHints(XmlName("xx"))
        val y = string.optional[Foo]("y", _.y).addHints(XmlName("yy"))
        struct(x, y)(Foo.apply)
      }
    }

    val xml = """|<Foo>
                  |  <xx>x</xx>
                  |  <yy>y</yy>
                  |</Foo>""".stripMargin

    testDecode(xml, Foo("x", Some("y")))
  }

  test("struct: attributes") {
    case class Foo(x: String, y: Option[String])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val x = string.required[Foo]("x", _.x).addHints(XmlAttribute())
        val y = string.optional[Foo]("y", _.y).addHints(XmlAttribute())
        struct(x, y)(Foo.apply)
      }
    }

    val xml = """<Foo x="x" y="y"/>""".stripMargin

    testDecode(xml, Foo("x", Some("y")))
  }

  test("struct: attributes with custom names") {
    case class Foo(x: String, y: Option[String])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val x =
          string.required[Foo]("x", _.x).addHints(XmlName("xx"), XmlAttribute())
        val y =
          string.optional[Foo]("y", _.y).addHints(XmlName("yy"), XmlAttribute())
        struct(x, y)(Foo.apply)
      }
    }

    val xml = """<Foo xx="x" yy="y"/>""".stripMargin

    testDecode(xml, Foo("x", Some("y")))
  }

  test("struct: empty optional attributes") {
    case class Foo(x: String, y: Option[String])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val x =
          string.required[Foo]("x", _.x).addHints(XmlAttribute())
        val y =
          string.optional[Foo]("y", _.y).addHints(XmlAttribute())
        struct(x, y)(Foo.apply)
      }
    }

    val xml = """<Foo x="x"/>""".stripMargin

    testDecode(xml, Foo("x", None))
  }

  test("list") {
    case class Foo(foos: List[Int])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val foos = list(int)
          .required[Foo]("foos", _.foos)
        struct(foos)(Foo.apply)
      }
    }

    val xml = """|<Foo>
                |   <foos>
                |      <member>1</member>
                |      <member>2</member>
                |      <member>3</member>
                |   </foos>
                |</Foo>""".stripMargin
    testDecode(xml, Foo(List(1, 2, 3)))
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

    val xml = """|<Foo>
                |   <foos>
                |      <x>1</x>
                |      <x>2</x>
                |      <x>3</x>
                |   </foos>
                |</Foo>""".stripMargin
    testDecode(xml, Foo(List(1, 2, 3)))
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

    val xml = """|<Foo>
                 |   <foos>1</foos>
                 |   <foos>2</foos>
                 |   <foos>3</foos>
                 |</Foo>
                 |""".stripMargin
    testDecode(xml, Foo(List(1, 2, 3)))
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

    val xml = """|<Foo>
                 |   <x>1</x>
                 |   <x>2</x>
                 |   <x>3</x>
                 |</Foo>
                 |""".stripMargin
    testDecode(xml, Foo(List(1, 2, 3)))
  }

  test("recursion") {
    case class Foo(foo: Option[Foo])
    object Foo {
      implicit val schema: Schema[Foo] = recursive {
        val foos = schema.optional[Foo]("foo", _.foo)
        struct(foos)(Foo.apply)
      }
    }

    val xml = """|<Foo>
                 |   <foo>
                 |      <foo>
                 |      </foo>
                 |   </foo>
                 |</Foo>
                 |""".stripMargin
    testDecode(xml, Foo(Some(Foo(Some(Foo(None))))))
  }

  def testDecode[A: Schema](xml: String, expected: A)(implicit
      loc: SourceLocation
  ): IO[Expectations] = {
    decode[A](xml).map(result => expect.same(result, expected))
  }

  private def decode[A: Schema](xmlString: String): IO[A] = {
    val decoder = implicitly[Schema[A]].compile(XmlSchemaVisitor)
    Stream
      .emit(xmlString)
      .through(events[IO, String]())
      .through(documents[IO, XmlDocument])
      .take(1)
      .compile
      .last
      .flatMap(_.liftTo[IO](new Throwable("BOOM")))
      .map(XmlCursor.fromDocument)
      .map(decoder.read(_).leftWiden[Throwable])
      .flatMap(_.liftTo[IO])
  }

}
