package smithy4s
package schema

import munit._
import smithy.api.TimestampFormat

final class DefaultValueSpec extends FunSuite {

  test("boolean") {
    testCase(Schema.boolean, false)
  }

  test("int") {
    testCase(Schema.int, 0)
  }

  test("long") {
    testCase(Schema.long, 0L)
  }

  test("short") {
    testCase(Schema.short, 0: Short)
  }

  test("float") {
    testCase(Schema.float, 0f)
  }

  test("double") {
    testCase(Schema.double, 0d)
  }

  test("big decimal") {
    testCase(Schema.bigdecimal, BigDecimal(0))
  }

  test("big int") {
    testCase(Schema.bigint, BigInt(0))
  }

  test("string") {
    testCase(Schema.string, "")
  }

  test("blob") {
    testCase(Schema.bytes, ByteArray(Array.empty))
  }

  test("timestamp - epoch") {
    testCase(Schema.timestamp, Timestamp(0, 0))
  }

  test("timestamp - date_time") {
    testCase(
      Schema.timestamp.addHints(TimestampFormat.DATE_TIME.widen),
      Timestamp(0, 0)
    )
  }

  test("timestamp - http_date") {
    testCase(
      Schema.timestamp.addHints(TimestampFormat.HTTP_DATE.widen),
      Timestamp(0, 0)
    )
  }

  test("list") {
    testCase(Schema.list(Schema.int), List.empty[Int])
  }

  test("map") {
    testCase(Schema.map(Schema.string, Schema.int), Map.empty[String, Int])
  }

  test("struct") {
    case class Foo(x: Int, y: Option[Int])
    val s = Schema.struct(
      Schema.int.required[Foo]("x", _.x),
      Schema.int.optional[Foo]("y", _.y)
    )(Foo.apply)
    testCaseOpt(s, None)
  }

  test("union") {
    type Foo = Either[Int, String]
    val left = Schema.int.oneOf[Foo]("left", Left(_))
    val right = Schema.string.oneOf[Foo]("right", Right(_))
    val u: Schema[Foo] = Schema.union(left, right) {
      case Left(int)     => left(int)
      case Right(string) => right(string)
    }
    testCaseOpt(u, None)
  }

  test("enumeration") {
    sealed abstract class FooBar(val stringValue: String, val intValue: Int)
        extends smithy4s.Enumeration.Value {
      val name = stringValue
      val value = stringValue
      val hints = Hints.empty
    }
    case object Foo extends FooBar("foo", 0)
    val e: Schema[FooBar] = Schema.enumeration[FooBar](List(Foo))
    testCaseOpt(e, None)
  }

  test("bijection") {
    case class Foo(x: Int)
    val b: Schema[Foo] = Schema.bijection(Schema.int, Foo(_), _.x)
    testCase(b, Foo(0))
  }

  test("refined") {
    val b: Schema[Int] =
      Schema.int.refined(smithy.api.Range(None, Option(BigDecimal(1))))
    testCaseOpt(b, None)
  }

  test("recursive") {
    case class Foo(foo: Option[Foo])
    object Foo {
      val f: Schema[Foo] = Schema.recursive {
        val foos = f.optional[Foo]("foo", _.foo)
        Schema.struct(foos)(Foo.apply)
      }
    }
    testCaseOpt(Foo.f, None)
  }

  private def testCaseOpt[A](schema: Schema[A], expect: Option[A])(implicit
      loc: Location
  ): Unit = {
    val sch = schema.addHints(smithy.api.Default(Document.DNull))
    val res = sch.getDefaultValue
    assertEquals(res, expect)
  }

  private def testCase[A](schema: Schema[A], expect: A)(implicit
      loc: Location
  ): Unit = testCaseOpt(schema, Some(expect))
}
