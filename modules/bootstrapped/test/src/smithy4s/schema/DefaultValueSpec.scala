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

final class DefaultValueSpec extends FunSuite {

  test("boolean") {
    testCaseOpt(Schema.boolean, None)
  }

  test("int") {
    testCaseOpt(Schema.int, None)
  }

  test("long") {
    testCaseOpt(Schema.long, None)
  }

  test("short") {
    testCaseOpt(Schema.short, None)
  }

  test("float") {
    testCaseOpt(Schema.float, None)
  }

  test("double") {
    testCaseOpt(Schema.double, None)
  }

  test("big decimal") {
    testCaseOpt(Schema.bigdecimal, None)
  }

  test("big int") {
    testCaseOpt(Schema.bigint, None)
  }

  test("string") {
    testCaseOpt(Schema.string, None)
  }

  test("blob") {
    testCaseOpt(Schema.bytes, None)
  }

  test("timestamp - epoch") {
    testCaseOpt(Schema.timestamp, None)
  }

  test("timestamp - date_time") {
    testCaseOpt(
      Schema.timestamp.addHints(TimestampFormat.DATE_TIME.widen),
      None
    )
  }

  test("timestamp - http_date") {
    testCaseOpt(
      Schema.timestamp.addHints(TimestampFormat.HTTP_DATE.widen),
      None
    )
  }

  test("list") {
    testCaseOpt(Schema.list(Schema.int), None)
  }

  test("map") {
    testCaseOpt(Schema.map(Schema.string, Schema.int), None)
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
    testCaseOpt(b, None)
  }

  test("refined") {
    val b: Schema[Int] =
      Schema.int.refined(smithy.api.Range(None, Option(BigDecimal(1))))
    testCaseOpt(b, Some(0))
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

  test("nullable") {
    val b: Schema[Nullable[Int]] = Schema.int.nullable
    testCaseOpt(b, Some(Nullable.Null))
  }

  private def testCaseOpt[A](schema: Schema[A], expect: Option[A])(implicit
      loc: Location
  ): Unit = {
    val sch = schema.addHints(smithy.api.Default(Document.DNull))
    val res = sch.getDefaultValue
    assertEquals(res, expect)
  }
}
