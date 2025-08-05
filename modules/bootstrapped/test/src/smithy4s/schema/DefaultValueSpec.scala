/*
 *  Copyright 2021-2025 Disney Streaming
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
package schema

import munit._
import smithy.api.TimestampFormat
import smithy4s.time.Timestamp
import java.time.Instant

final class DefaultValueSpec extends FunSuite {

  test("boolean") {
    testCaseOpt(Schema.boolean, None)
    testCase(Schema.boolean, Document.fromBoolean(true), true)
  }

  test("int") {
    testCaseOpt(Schema.int, None)
    testCase(Schema.int, Document.fromInt(42), 42)
  }

  test("long") {
    testCaseOpt(Schema.long, None)
    testCase(Schema.long, Document.fromLong(42), 42L)
  }

  test("short") {
    testCaseOpt(Schema.short, None)
    testCase(Schema.short, Document.fromInt(42), 42: Short)
  }

  test("float") {
    testCaseOpt(Schema.float, None)
    testCase(Schema.float, Document.fromDouble(42), 42f)
  }

  test("double") {
    testCaseOpt(Schema.double, None)
    testCase(Schema.double, Document.fromDouble(42.0), 42.0)
  }

  test("big decimal") {
    testCaseOpt(Schema.bigdecimal, None)
    testCase(
      Schema.bigdecimal,
      Document.fromBigDecimal(BigDecimal(42)),
      BigDecimal(42)
    )
  }

  test("big int") {
    testCaseOpt(Schema.bigint, None)
    testCase(
      Schema.bigint,
      Document.fromBigDecimal(BigDecimal(BigInt(42))),
      BigInt(42)
    )
  }

  test("string") {
    testCaseOpt(Schema.string, None)
    testCase(Schema.string, Document.fromString("42"), "42")
  }

  test("blob") {
    testCaseOpt(Schema.bytes, None)
  }

  test("timestamp - epoch") {
    testCaseOpt(Schema.timestamp, None)
    val ts = Timestamp.fromEpochSecond(Instant.now().getEpochSecond())
    testCase(
      Schema.timestamp,
      Document.fromLong(ts.epochSecond),
      ts
    )
  }

  test("timestamp - date_time") {
    val s = Schema.timestamp.addHints(TimestampFormat.DATE_TIME.widen)
    testCaseOpt(s, None)
    testCase(
      s,
      Document.fromString("1985-04-12T23:20:50.52Z"),
      Timestamp.parse("1985-04-12T23:20:50.52Z", TimestampFormat.DATE_TIME).get
    )
  }

  test("timestamp - http_date") {
    val s = Schema.timestamp.addHints(TimestampFormat.HTTP_DATE.widen)
    testCaseOpt(s, None)
    testCase(
      s,
      Document.fromString("Tue, 29 Apr 2014 18:30:38 GMT"),
      Timestamp
        .parse(
          "Tue, 29 Apr 2014 18:30:38 GMT",
          TimestampFormat.HTTP_DATE
        )
        .get
    )
  }

  test("list") {
    testCaseOpt(Schema.list(Schema.int), None)
    testCase(
      Schema.list(Schema.int),
      Document.array(Document.fromInt(42), Document.fromInt(43)),
      List(42, 43)
    )
  }

  test("map") {
    testCaseOpt(Schema.map(Schema.string, Schema.int), None)
    testCase(
      Schema.map(Schema.string, Schema.int),
      Document.obj("x" -> Document.fromInt(42), "y" -> Document.fromInt(43)),
      Map("x" -> 42, "y" -> 43)
    )
  }

  test("struct") {
    case class Foo(x: Int, y: Option[Int])
    val s = Schema.struct(
      Schema.int.required[Foo]("x", _.x),
      Schema.int.optional[Foo]("y", _.y)
    )(Foo.apply)
    testCaseOpt(s, None)
    testCase(
      s,
      Document.obj("x" -> Document.fromInt(42), "y" -> Document.DNull),
      Foo(42, None)
    )
  }

  test("enumeration") {
    sealed abstract class FooBar(val stringValue: String, val intValue: Int)
        extends smithy4s.Enumeration.Value {
      type EnumType = FooBar
      val id: ShapeId = ShapeId("test", "FooBar")
      val name = stringValue
      val value = stringValue
      val hints = Hints.empty
      def enumeration: Enumeration[FooBar] = FooBar
    }
    object FooBar extends smithy4s.Enumeration[FooBar] {
      case object Foo extends FooBar("foo", 0)
      def id: ShapeId = ShapeId("test", "FooBar")
      val hints = Hints.empty
      def values: List[FooBar] = List(Foo)
      val schema: Schema[FooBar] = Schema.stringEnumeration[FooBar](values)
    }

    testCaseOpt(FooBar.schema, None)
    testCase(FooBar.schema, Document.fromString("foo"), FooBar.Foo)
  }

  test("bijection") {
    case class Foo(x: Int)
    val b: Schema[Foo] = Schema.bijection(Schema.int, Foo(_), _.x)
    testCaseOpt(b, None)
    testCase(b, Document.fromInt(42), Foo(42))
  }

  test("refined") {
    val b: Schema[Int] =
      Schema.int.refined(smithy.api.Range(None, Option(BigDecimal(1))))
    testCaseOpt(b, None)
    testCase(b, Document.fromInt(1), 1)
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
    testCase(
      Foo.f,
      Document.obj("foo" -> Document.obj("foo" -> Document.DNull)),
      Foo(Some(Foo(None)))
    )
  }

  test("nullable") {
    val b: Schema[Nullable[Int]] = Schema.int.nullable
    testCaseOpt(b, Some(Nullable.Null))
    testCase(b, Document.fromInt(42), Nullable.Value(42))
  }

  private def testCaseOpt[A](schema: Schema[A], expect: Option[A])(implicit
      loc: Location
  ): Unit = {
    val sch = schema.addHints(smithy.api.Default(Document.DNull))
    val res = sch.getDefaultValue
    assertEquals(res, expect)
  }

  private def testCase[A](schema: Schema[A], document: Document, expect: A)(
      implicit loc: Location
  ): Unit = {
    val sch = schema.addHints(smithy.api.Default(document))
    val res = sch.getDefaultValue
    assertEquals(res, Some(expect))
  }
}
