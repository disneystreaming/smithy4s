/*
 *  Copyright 2021-2024 Disney Streaming
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
    testCase(Schema.bytes, Blob.empty)
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
    val u: Schema[Foo] = Schema.either(Schema.int, Schema.string)
    testCaseOpt(u, None)
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
