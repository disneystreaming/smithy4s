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

package smithy4s.compliancetests

import cats.kernel.Eq
import smithy4s.ByteArray
import smithy4s.Hints
import smithy4s.ShapeId
import smithy4s.Timestamp
import smithy4s.compliancetests.internals.eq.EqSchemaVisitor
import smithy4s.schema.Schema
import smithy4s.schema.Schema._
import smithy4s.schema.SchemaVisitor
import weaver.Expectations
import weaver.FunSuite

object EqVisitorSpec extends FunSuite {

  val visitor: SchemaVisitor[Eq] = EqSchemaVisitor
  test("int") {
    implicit val schema: Schema[Int] = int
    val int0 = 1
    val int1 = 1
    val int2 = 2
    schemaEq(int0, int1)(int2)
  }

  test("string") {
    implicit val schema: Schema[String] = string
    val foo = "foo"
    val foo1 = "foo"
    val neq = "neq"
    schemaEq(foo, foo1)(neq)
  }

  test("boolean") {
    implicit val schema: Schema[Boolean] = boolean
    val foo = true
    val foo1 = true
    val neq = false
    schemaEq(foo, foo1)(neq)
  }

  test("long") {
    implicit val schema: Schema[Long] = long
    val foo = 1L
    val foo1 = 1L
    val neq = 2L
    schemaEq(foo, foo1)(neq)

  }

  test("short") {
    implicit val schema: Schema[Short] = short
    val foo = 1.toShort
    val foo1 = 1.toShort
    val neq = 2.toShort
    schemaEq(foo, foo1)(neq)
  }

  test("byte") {
    implicit val schema: Schema[Byte] = byte
    val foo = 1.toByte
    val foo1 = 1.toByte
    val neq = 2.toByte
    schemaEq(foo, foo1)(neq)
  }

  test("double") {
    implicit val schema: Schema[Double] = double
    val foo = 1.0
    val foo1 = 1.0
    val neq = 2.0
    schemaEq(foo, foo1)(neq)
  }

  test("float") {
    implicit val schema: Schema[Float] = float
    val foo = 1.0f
    val foo1 = 1.0f
    val neq = 2.0f
    schemaEq(foo, foo1)(neq)
  }

  test("bigint") {
    implicit val schema: Schema[BigInt] = bigint
    val foo = BigInt(1)
    val foo1 = BigInt(1)
    val neq = BigInt(2)
    schemaEq(foo, foo1)(neq)
  }

  test("bigdecimal") {
    implicit val schema: Schema[BigDecimal] = bigdecimal
    val foo = BigDecimal(1)
    val foo1 = BigDecimal(1)
    val neq = BigDecimal(2)
    schemaEq(foo, foo1)(neq)

  }

  test("smithy4s ByteArray") {
    implicit val schema: Schema[ByteArray] = bytes
    val fooBar = ByteArray("fooBar".getBytes)
    val fooBar1 = ByteArray("fooBar".getBytes)
    val neqFoo = ByteArray("neqFoo".getBytes)
    schemaEq(fooBar, fooBar1)(neqFoo)
  }

  test("smithy4s timestamp") {
    implicit val schema: Schema[Timestamp] = timestamp
    val now = java.time.Instant.now()
    val foo = Timestamp.fromEpochSecond(now.getEpochSecond)
    val foo1 = Timestamp.fromEpochSecond(now.getEpochSecond)
    val neq = Timestamp.fromEpochSecond(now.getEpochSecond + 1)
    schemaEq(foo, foo1)(neq)
  }

  test("struct") {
    case class Foo(x: String, y: Option[String])
    object Foo {
      implicit val schema: Schema[Foo] = {
        StructSchema(
          ShapeId("", "Foo"),
          Hints.empty,
          Vector(
            string.required[Foo]("x", _.x),
            string.optional[Foo]("y", _.y)
          ),
          arr =>
            Foo.apply(
              arr(0).asInstanceOf[String],
              arr(1).asInstanceOf[Option[String]]
            )
        )

      }
    }
    val foo = Foo("foo", Some("neq"))
    val foo1 = Foo("foo", Some("neq"))
    val neq = Foo("neq", Some("foo"))
    schemaEq(foo, foo1)(neq)
  }

  test("struct: empty optional") {
    case class Foo(x: String, y: Option[String])
    object Foo {
      implicit val schema: Schema[Foo] = {
        StructSchema(
          ShapeId("", "Foo"),
          Hints.empty,
          Vector(
            string.required[Foo]("x", _.x),
            string.optional[Foo]("y", _.y)
          ),
          arr =>
            Foo.apply(
              arr(0).asInstanceOf[String],
              arr(1).asInstanceOf[Option[String]]
            )
        )

      }
    }

    val foo = Foo("foo", None)
    val foo1 = Foo("foo", None)
    val neq = Foo("foo", Some("foo"))
    schemaEq(foo, foo1)(neq)

  }

  test("list") {
    case class Foo(foos: List[Int])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val foos = list(int)
          .required[Foo]("foos", _.foos)
        struct(foos)(Foo.apply)
      }.withId(ShapeId("", "Foo"))
    }
    val foo = Foo(List(1, 2, 3))
    val foo1 = Foo(List(1, 2, 3))
    val neq = Foo(List(1, 2, 4))
    schemaEq(foo, foo1)(neq)
  }

  test("set") {
    case class Foo(foos: Set[Int])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val foos = set(int)
          .required[Foo]("foos", _.foos)
        struct(foos)(Foo.apply)
      }.withId(ShapeId("", "Foo"))
    }
    val foo = Foo(Set(1, 2, 3))
    val foo1 = Foo(Set(1, 2, 3))
    val neq = Foo(Set(1, 2, 4))
    schemaEq(foo, foo1)(neq)

  }

  test("vector") {
    case class Foo(foos: Vector[Int])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val foos = vector(int)
          .required[Foo]("foos", _.foos)
        struct(foos)(Foo.apply)
      }.withId(ShapeId("", "Foo"))
    }
    val foo = Foo(Vector(1, 2, 3))
    val foo1 = Foo(Vector(1, 2, 3))
    val neq = Foo(Vector(1, 2, 4))
    schemaEq(foo, foo1)(neq)

  }
  test("indexedSeq") {
    case class Foo(foos: IndexedSeq[Int])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val foos = indexedSeq(int)
          .required[Foo]("foos", _.foos)
        struct(foos)(Foo.apply)
      }.withId(ShapeId("", "Foo"))
    }
    val foo = Foo(IndexedSeq(1, 2, 3))
    val foo1 = Foo(IndexedSeq(1, 2, 3))
    val neq = Foo(IndexedSeq(1, 2, 4))
    schemaEq(foo, foo1)(neq)

  }

  test("map") {
    case class Foo(foos: Map[String, Int])
    object Foo {
      implicit val schema: Schema[Foo] = {
        val foos = map(string, int)
          .required[Foo]("foos", _.foos)
        struct(foos)(Foo.apply)
      }.withId(ShapeId("", "Foo"))
    }
    val foo = Foo(Map("foo" -> 1, "bar" -> 2))
    val foo1 = Foo(Map("foo" -> 1, "bar" -> 2))
    val neq = Foo(Map("foo" -> 1, "neq" -> 3))
    schemaEq(foo, foo1)(neq)

  }

  test("recursion") {
    case class Foo(foo: Option[Foo])
    object Foo {
      implicit val schema: Schema[Foo] = recursive {
        val foos = schema.optional[Foo]("foo", _.foo)
        struct(foos)(Foo.apply)
      }.withId(ShapeId("", "Foo"))
    }

    val foo = Foo(Some(Foo(None)))
    val foo1 = Foo(Some(Foo(None)))
    val neq = Foo(Some(Foo(Some(Foo(None)))))
    schemaEq(foo, foo1)(neq)

  }

  test("union") {
    sealed trait IntOrString
    case class IntValue(value: Int) extends IntOrString
    case class StringValue(value: String) extends IntOrString
    implicit val schema: Schema[IntOrString] = {
      val intValue = int.oneOf[IntOrString]("intValue", IntValue(_))
      val stringValue = string.oneOf[IntOrString]("stringValue", StringValue(_))
      union(intValue, stringValue) {
        case IntValue(int)       => intValue(int)
        case StringValue(string) => stringValue(string)
      }.withId(ShapeId("", "Foo"))
    }
    val foo0: IntOrString = IntValue(1)
    val foo1: IntOrString = IntValue(1)
    val neq: IntOrString = IntValue(2)
    schemaEq(foo0, foo1)(neq)

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

      case object Bar extends FooBar("neq", 1)

      implicit val schema: Schema[FooBar] =
        enumeration[FooBar](List(Foo, Bar))
    }
    val foo: FooBar = FooBar.Foo
    val foo1: FooBar = FooBar.Foo
    val neq: FooBar = FooBar.Bar
    schemaEq(foo, foo1)(neq)

  }

  def schemaEq[A: Schema](a0: A, a1: A)(a2: A): Expectations = {
    val eq = visitor(implicitly[Schema[A]])
    expect(eq.eqv(a0, a1)) and expect(eq.neqv(a0, a2))
  }
}
