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

package smithy4s.interopcats

import cats.Show
import smithy4s.{ByteArray, ShapeId, Timestamp}
import smithy4s.schema.Schema
import smithy4s.schema.Schema._
import weaver.FunSuite
import smithy4s.interopcats.testcases.FooBar
import smithy4s.interopcats.testcases.IntOrString.schema
import smithy4s.interopcats.testcases.IntOrString._

object ShowVisitorSpec extends FunSuite with CompatProvider {

  def schemaVisitorShow[A]: Schema[A] => Show[A] =
    SchemaVisitorShow.fromSchema(_)

  test("int") {
    val schema: Schema[Int] = int
    val intValue = 1
    val showOutput = schemaVisitorShow(schema).show(intValue)
    expect.eql(showOutput, "1")
  }

  test("string") {
    val schema: Schema[String] = string
    val foo = "foo"
    val showOutput = schemaVisitorShow(schema).show(foo)
    expect.eql(showOutput, "foo")
  }

  test("boolean") {
    val schema: Schema[Boolean] = boolean
    val foo = true
    val showOutput = schemaVisitorShow(schema).show(foo)
    expect.eql(showOutput, "true")
  }

  test("long") {
    val schema: Schema[Long] = long
    val foo = 1L
    val showOutput = schemaVisitorShow(schema).show(foo)
    expect.eql(showOutput, "1")
  }

  test("short") {
    val schema: Schema[Short] = short
    val foo = 1.toShort
    val showOutput = schemaVisitorShow(schema).show(foo)
    expect.eql(showOutput, "1")
  }

  test("byte") {
    val schema: Schema[Byte] = byte
    val foo = 1.toByte
    val showOutput = schemaVisitorShow(schema).show(foo)
    expect.eql(showOutput, "1")

  }

  test("double") {
    val schema: Schema[Double] = double
    val foo = 1.0
    val showOutput = schemaVisitorShow(schema).show(foo)
    expect.eql(showOutput, foo.toString)

  }

  test("float") {
    val schema: Schema[Float] = float
    val foo = 1.0f
    val showOutput = schemaVisitorShow(schema).show(foo)
    expect.eql(showOutput, foo.toString)
  }

  test("bigint") {
    val schema: Schema[BigInt] = bigint
    val foo = BigInt(1)
    val showOutput = schemaVisitorShow(schema).show(foo)
    expect.eql(showOutput, "1")
  }

  test("bigdecimal") {
    val schema: Schema[BigDecimal] = bigdecimal
    val foo = BigDecimal(1)
    val showOutput = schemaVisitorShow(schema).show(foo)
    expect.eql(showOutput, "1")

  }

  test("smithy4s ByteArray") {
    val schema: Schema[ByteArray] = bytes
    val fooBar = ByteArray("fooBar".getBytes)
    val showOutput = schemaVisitorShow(schema).show(fooBar)
    expect.eql(showOutput, "Zm9vQmFy")
  }

  test("smithy4s timestamp") {
    val schema: Schema[Timestamp] = timestamp
    val foo = getTimestamp
    val showOutput = schemaVisitorShow(schema).show(foo)
    expect.eql(showOutput, foo.toString)
  }

  test("struct") {
    case class Foo(x: String, y: Option[String])
    object Foo {
      val schema: Schema[Foo] = {
        struct
          .apply(
            string.required[Foo]("x", _.x),
            string.optional[Foo]("y", _.y)
          )(Foo.apply)
          .withId(ShapeId("", "Foo"))

      }
    }
    val foo = Foo("foo", Some("bar"))
    val showOutput = schemaVisitorShow(Foo.schema).show(foo)
    expect.eql(showOutput, "Foo(x = foo, y = Some(bar))")
  }
  test("struct: empty optional") {
    case class Foo(x: String, y: Option[String])
    object Foo {
      val schema: Schema[Foo] = {
        struct
          .apply(
            string.required[Foo]("x", _.x),
            string.optional[Foo]("y", _.y)
          )(Foo.apply)
          .withId(ShapeId("", "Foo"))

      }
    }

    val foo = Foo("foo", None)
    val showOutput = schemaVisitorShow(Foo.schema).show(foo)
    expect.eql(showOutput, "Foo(x = foo, y = None)")
  }

  test("list") {
    case class Foo(foos: List[Int])
    object Foo {
      val schema: Schema[Foo] = {
        val foos = list(int)
          .required[Foo]("foos", _.foos)
        struct(foos)(Foo.apply)
      }.withId(ShapeId("", "Foo"))
    }
    val foo = Foo(List(1, 2, 3))
    val showOutput = schemaVisitorShow(Foo.schema).show(foo)
    expect.eql(showOutput, "Foo(foos = List(1, 2, 3))")
  }

  test("set") {
    case class Foo(foos: Set[Int])
    object Foo {
      val schema: Schema[Foo] = {
        val foos = set(int)
          .required[Foo]("foos", _.foos)
        struct(foos)(Foo.apply)
      }.withId(ShapeId("", "Foo"))
    }
    val foo = Foo(Set(1, 2, 3))
    val showOutput = schemaVisitorShow(Foo.schema).show(foo)
    expect.eql(showOutput, "Foo(foos = Set(1, 2, 3))")
  }
  test("vector") {
    case class Foo(foos: Vector[Int])
    object Foo {
      val schema: Schema[Foo] = {
        val foos = vector(int)
          .required[Foo]("foos", _.foos)
        struct(foos)(Foo.apply)
      }.withId(ShapeId("", "Foo"))
    }
    val foo = Foo(Vector(1, 2, 3))
    val showOutput = schemaVisitorShow(Foo.schema).show(foo)
    expect.eql(showOutput, "Foo(foos = Vector(1, 2, 3))")
  }
  test("indexedSeq") {
    case class Foo(foos: IndexedSeq[Int])
    object Foo {
      val schema: Schema[Foo] = {
        val foos = indexedSeq(int)
          .required[Foo]("foos", _.foos)
        struct(foos)(Foo.apply)
      }.withId(ShapeId("", "Foo"))
    }
    val foo = Foo(IndexedSeq(1, 2, 3))
    val showOutput = schemaVisitorShow(Foo.schema).show(foo)
    expect.eql(showOutput, "Foo(foos = IndexedSeq(1, 2, 3))")
  }

  test("map") {
    case class Foo(foos: Map[String, Int])
    object Foo {
      val schema: Schema[Foo] = {
        val foos = map(string, int)
          .required[Foo]("foos", _.foos)
        struct(foos)(Foo.apply)
      }.withId(ShapeId("", "Foo"))
    }
    val foo = Foo(Map("foo" -> 1, "bar" -> 2))
    val showOutput = schemaVisitorShow(Foo.schema).show(foo)
    expect.eql(showOutput, "Foo(foos = Map(foo -> 1, bar -> 2))")
  }

  test("recursion") {
    case class Foo(foo: Option[Foo])
    object Foo {
      val schema: Schema[Foo] =
        recursive {
          val foos = schema.optional[Foo]("foo", _.foo)
          struct(foos)(Foo.apply)
        }.withId(ShapeId("", "Foo"))
    }

    val foo = Foo(Some(Foo(None)))
    val showOutput = schemaVisitorShow(Foo.schema).show(foo)
    expect.eql(showOutput, "Foo(foo = Some(Foo(foo = None)))")

  }

  test("union") {
    val foo0 = IntValue(1)
    val foo1 = StringValue("foo")
    val showOutput0 = schemaVisitorShow(schema).show(foo0)
    val showOutput1 = schemaVisitorShow(schema).show(foo1)
    expect.eql(showOutput0, "IntOrString(intValue = 1)")
    expect.eql(showOutput1, "IntOrString(stringValue = foo)")

  }

  test("enumeration") {

    val foo = FooBar.Foo
    val showOutput = schemaVisitorShow(FooBar.schema).show(foo)
    val bar = FooBar.Bar
    val showOutput1 = schemaVisitorShow(FooBar.schema).show(bar)
    expect.eql(showOutput, foo.stringValue)
    expect.eql(showOutput1, bar.stringValue)
  }
}
