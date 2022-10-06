package smithy4s.xml

import smithy4s.xml.internals.XmlDecoderSchemaVisitor
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
import smithy4s.Hints
import smithy4s.ShapeId

object XmlDecodingSpec extends SimpleIOSuite {

  test("int") {
    implicit val schema: Schema[Int] = int
    val xml = "<x>1</x>"
    checkContent(xml, 1)
  }

  test("string") {
    implicit val schema: Schema[String] = string
    val xml = "<x>foo</x>"
    checkContent(xml, "foo")
  }

  test("boolean") {
    implicit val schema: Schema[Boolean] = boolean
    val xml = "<x>true</x>"
    checkContent(xml, true)
  }

  test("long") {
    implicit val schema: Schema[Long] = long
    val xml = "<x>1</x>"
    checkContent(xml, 1L)
  }

  test("short") {
    implicit val schema: Schema[Short] = short
    val xml = "<x>1</x>"
    checkContent(xml, 1.toShort)
  }

  test("byte") {
    implicit val schema: Schema[Byte] = byte
    val xml = "<x>99</x>"
    checkContent(xml, 'c'.toByte)
  }

  test("double") {
    implicit val schema: Schema[Double] = double
    val xml = "<x>1.1</x>"
    checkContent(xml, 1.1)
  }

  test("float") {
    implicit val schema: Schema[Float] = float
    val xml = "<x>1.1</x>"
    checkContent(xml, 1.1f)
  }

  test("bigint") {
    implicit val schema: Schema[BigInt] = bigint
    val xml =
      "<x>1000000000000000000000000000000000000000000000000000000000000000</x>"
    checkContent(
      xml,
      BigInt("1000000000000000000000000000000000000000000000000000000000000000")
    )
  }

  test("bigdecimal") {
    implicit val schema: Schema[BigDecimal] = bigdecimal
    val xml =
      "<x>1000000000000000000000000000000000000000000000000000000000000000.1</x>"
    checkContent(
      xml,
      BigDecimal(
        "1000000000000000000000000000000000000000000000000000000000000000.1"
      )
    )
  }

  test("bytes") {
    implicit val schema: Schema[ByteArray] = bytes
    val xml = "<x>Zm9vYmFy</x>"
    checkContent(xml, ByteArray("foobar".getBytes()))
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

    checkContent(xml, Foo("x", Some("y")))
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

    checkContent(xml, Foo("x", None))
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

    checkContent(xml, Foo("x", Some("y")))
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

    checkContent(xml, Foo("x", Some("y")))
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

    checkContent(xml, Foo("x", Some("y")))
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

    checkContent(xml, Foo("x", None))
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
    checkContent(xml, Foo(List(1, 2, 3)))
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
    checkContent(xml, Foo(List(1, 2, 3)))
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
    checkContent(xml, Foo(List(1, 2, 3)))
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
    checkContent(xml, Foo(List(1, 2, 3)))
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
    val xmlLeft = """<left>1</left>"""
    val xmlRight = """<right>"hello"</right>""".stripMargin
    checkContent[Foo](xmlLeft, Left(1)) <+>
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
    val xmlLeft = """<foo>1</foo>"""
    val xmlRight = """<bar>"hello"</bar>""".stripMargin
    checkContent[Foo](xmlLeft, Left(1)) <+>
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
      implicit val schema: Schema[FooBar] = enumeration[FooBar](List(Foo, Bar))
    }
    val xmlFoo = "<x>foo</x>"
    val xmlBar = "<x>bar</x>"
    checkContent[FooBar](xmlFoo, FooBar.Foo) <+>
      checkContent[FooBar](xmlBar, FooBar.Bar)
  }

  test("map") {
    case class Foo(foos: Map[String, Int])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val foos = map(string, int)
          .required[Foo]("foos", _.foos)
          .addHints(XmlName("entries"))
        struct(foos)(Foo.apply)
      }
    }

    val xml = """|<Foo>
                 |   <entries>
                 |        <entry>
                 |            <key>a</key>
                 |            <value>1</value>
                 |        </entry>
                 |        <entry>
                 |            <key>b</key>
                 |            <value>2</value>
                 |        </entry>
                 |   </entries>
                 |</Foo>""".stripMargin
    checkContent(xml, Foo(Map("a" -> 1, "b" -> 2)))
  }

  test("map: custom names") {
    case class Foo(foos: Map[String, Int])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val foos =
          map(string.addHints(XmlName("k")), int.addHints(XmlName("v")))
            .required[Foo]("foos", _.foos)
        struct(foos)(Foo.apply)
      }
    }

    val xml = """|<Foo>
                 |   <foos>
                 |        <entry>
                 |            <k>a</k>
                 |            <v>1</v>
                 |        </entry>
                 |        <entry>
                 |            <k>b</k>
                 |            <v>2</v>
                 |        </entry>
                 |   </foos>
                 |</Foo>""".stripMargin
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
        struct(foos)(Foo.apply)
      }
    }

    val xml = """|<Foo>
                 |   <foos>
                 |      <key>a</key>
                 |      <value>1</value>
                 |   </foos>
                 |   <foos>
                 |      <key>b</key>
                 |      <value>2</value>
                 |   </foos>
                 |</Foo>""".stripMargin
    checkContent(xml, Foo(Map("a" -> 1, "b" -> 2)))
  }

  test("Document decoding") {
    case class Foo(x: Int)
    object Foo {
      implicit val schema: Schema[Foo] = {
        val x = int.required[Foo]("x", _.x)
        struct(x)(Foo.apply)
      }.withId(ShapeId("foo", "Foo"))
    }

    val xml = """|<Foo>
                 |   <x>1</x>
                 |</Foo>""".stripMargin
    checkDocument(xml, Foo(1))
  }

  test("Document decoding: custom name") {
    case class Foo(x: Int)
    object Foo {
      implicit val schema: Schema[Foo] = {
        val x = int.required[Foo]("x", _.x)
        struct(x)(Foo.apply).addHints(XmlName("F"))
      }
    }

    val xml = """|<F>
                 |   <x>1</x>
                 |</F>""".stripMargin
    checkDocument(xml, Foo(1))
  }

  test("Document decoding: failure") {
    case class Foo(x: Int)
    object Foo {
      implicit val schema: Schema[Foo] = {
        val x = int.required[Foo]("x", _.x)
        struct(x)(Foo.apply)
      }.withId(ShapeId("foo", "Foo"))
    }

    val xml = """|<Bar>
                 |   <x>1</x>
                 |</Bar>""".stripMargin
    decodeDocument[Foo](xml).attempt.map { result =>
      expect.same(
        result,
        Left(
          XmlDecodeError(XPath.root, "Expected Foo XML root element, got Bar")
        )
      )
    }
  }

  def checkContent[A: Schema](xml: String, expected: A)(implicit
      loc: SourceLocation
  ): IO[Expectations] = {
    decodeContent[A](xml).map(result => expect.same(result, expected))
  }

  def checkDocument[A: Schema](xml: String, expected: A)(implicit
      loc: SourceLocation
  ): IO[Expectations] = {
    decodeDocument[A](xml).map(result => expect.same(result, expected))
  }

  // Decode document differs from decode content in that the top-level
  // tag is checked against the ShapeId
  private def decodeDocument[A: Schema](xmlString: String): IO[A] = {
    val decoder = XmlDocument.Decoder.fromSchema(implicitly[Schema[A]])
    parseDocument(xmlString)
      .map(decoder.decode(_).leftWiden[Throwable])
      .flatMap(_.liftTo[IO])
  }

  private def decodeContent[A: Schema](xmlString: String): IO[A] = {
    val decoder = implicitly[Schema[A]].compile(XmlDecoderSchemaVisitor)
    parseDocument(xmlString)
      .map(XmlCursor.fromDocument)
      .map(decoder.decode(_).leftWiden[Throwable])
      .flatMap(_.liftTo[IO])
  }

  private def parseDocument(xmlString: String): IO[XmlDocument] = {
    Stream
      .emit(xmlString)
      .through(events[IO, String]())
      .through(documents[IO, XmlDocument])
      .take(1)
      .compile
      .last
      .flatMap(_.liftTo[IO](new Throwable("BOOM")))
  }

}
