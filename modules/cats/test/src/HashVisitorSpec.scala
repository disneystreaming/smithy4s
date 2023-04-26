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

import cats.Hash
import smithy4s.schema.Schema._
import smithy4s.schema.{Schema, SchemaVisitor}
import smithy4s.{ByteArray, Hints, ShapeId, Timestamp}
import smithy4s.interopcats.testcases.FooBar
import smithy4s.interopcats.testcases._
import smithy4s.interopcats.testcases.IntOrString._
import smithy4s.interopcats.testcases.IntOrInt
import weaver.FunSuite
import scala.util.hashing.MurmurHash3.productSeed
import HashTestUtils._

object HashVisitorSpec extends FunSuite {

  val visitor: SchemaVisitor[Hash] = SchemaVisitorHash

  test("int") {
    val schema: Schema[Int] = int
    val intValue = 1
    val hashOutput = visitor(schema).hash(intValue)
    expect.eql(intValue.hashCode, hashOutput)
  }

  test("string") {
    val schema: Schema[String] = string
    val foo = "foo"
    val hashOutput = visitor(schema).hash(foo)
    expect.eql("foo".hashCode, hashOutput)
  }

  test("boolean") {
    val schema: Schema[Boolean] = boolean
    val foo = true
    val hashOutput = visitor(schema).hash(foo)
    expect.eql(foo.hashCode(), hashOutput)
  }

  test("long") {
    val schema: Schema[Long] = long
    val foo = 1L
    val hashOutput = visitor(schema).hash(foo)
    expect.eql(foo.hashCode(), hashOutput)
  }

  test("short") {
    val schema: Schema[Short] = short
    val foo = 1.toShort
    val hashOutput = visitor(schema).hash(foo)
    expect.eql(foo.hashCode(), hashOutput)
  }

  test("byte") {
    val schema: Schema[Byte] = byte
    val foo = 1.toByte
    val hashOutput = visitor(schema).hash(foo)
    expect.eql(foo.hashCode(), hashOutput)

  }

  test("double") {
    val schema: Schema[Double] = double
    val foo = 1.0
    val hashOutput = visitor(schema).hash(foo)
    expect.eql(foo.hashCode(), hashOutput)

  }

  test("float") {
    val schema: Schema[Float] = float
    val foo = 1.0f
    val hashOutput = visitor(schema).hash(foo)
    expect.eql(foo.hashCode(), hashOutput)
  }

  test("bigint") {
    val schema: Schema[BigInt] = bigint
    val foo = BigInt(1)
    val hashOutput = visitor(schema).hash(foo)
    expect.eql(foo.hashCode(), hashOutput)
  }

  test("bigdecimal") {
    val schema: Schema[BigDecimal] = bigdecimal
    val foo = BigDecimal(1)
    val hashOutput = visitor(schema).hash(foo)
    expect.eql(foo.hashCode(), hashOutput)

  }

  test("smithy4s ByteArray") {
    val schema: Schema[ByteArray] = bytes
    val fooBar = ByteArray("fooBar".getBytes)
    val hashOutput = visitor(schema).hash(fooBar)
    expect.eql(fooBar.array.hashCode(), hashOutput)
  }

  test("smithy4s timestamp") {
    val schema: Schema[Timestamp] = timestamp
    val now = java.time.Instant.now()
    val foo = Timestamp.fromEpochSecond(now.getEpochSecond)
    val hashOutput = visitor(schema).hash(foo)
    expect.eql(foo.hashCode(), hashOutput)
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
    val intList = List(1, 2, 3)
    val foo = Foo(intList)
    val expectedHash = testStructHash("Foo", intList)
    val hashOutput = visitor(Foo.schema).hash(foo)
    expect.eql(expectedHash, hashOutput)
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
    val intSet = Set(1, 2, 3)
    val foo = Foo(intSet)
    val expectedHash = testStructHash("Foo", intSet)
    val hashOutput = visitor(Foo.schema).hash(foo)
    expect.eql(expectedHash, hashOutput)
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
    val intVector = Vector(1, 2, 3)
    val foo = Foo(intVector)
    val expectedHash = testStructHash("Foo", intVector)
    val hashOutput = visitor(Foo.schema).hash(foo)
    expect.eql(expectedHash, hashOutput)
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
    val intIndexedSeq = IndexedSeq(1, 2, 3)
    val foo = Foo(intIndexedSeq)
    val expectedHash = testStructHash("Foo", intIndexedSeq.toList)
    val hashOutput = visitor(Foo.schema).hash(foo)
    expect.eql(expectedHash, hashOutput)
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
    val intMap = Map("foo" -> 1, "bar" -> 2)
    val foo = Foo(intMap)
    val expectedHash = testStructHash("Foo", intMap)
    val hashOutput = visitor(Foo.schema).hash(foo)
    expect.eql(expectedHash, hashOutput)
  }

  test("struct") {
    case class Foo(x: String, y: String)
    object Foo {
      val schema: Schema[Foo] = {
        StructSchema(
          ShapeId("", "Foo"),
          Hints.empty,
          Vector(
            string.required[Foo]("x", _.x),
            string.required[Foo]("y", _.y)
          ),
          arr =>
            Foo.apply(
              arr(0).asInstanceOf[String],
              arr(1).asInstanceOf[String]
            )
        )

      }
    }
    val foo = Foo("foo", "bar")
    val expectedHash = testStructHash("Foo", "foo", "bar")
    val hashOutput = visitor(Foo.schema).hash(foo)
    expect.eql(expectedHash, hashOutput)
  }
  test("struct: empty optional") {
    case class Foo(x: String, y: Option[String])
    object Foo {
      val schema: Schema[Foo] = {
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
    val opHash = Hash[Option[String]].hash(None)
    val expectedHash =
      combineHash(productSeed, "Foo".hashCode, "foo".hashCode, opHash)
    val hashOutput = visitor(Foo.schema).hash(foo)
    expect.eql(expectedHash, hashOutput)
  }

  test("recursion") {
    val foo = RecursiveFoo(Some(RecursiveFoo(None)))
    val expectedHash = Hash[RecursiveFoo].hash(foo)
    val hashOutput = visitor(RecursiveFoo.schema).hash(foo)
    expect.eql(expectedHash, hashOutput)
  }

  test("union with different subtypes") {
    val foo0 = IntValue(1)
    val foo1 = StringValue("foo")
    val hashOutput0 = visitor(IntOrString.schema).hash(foo0)
    val hashOutput1 = visitor(IntOrString.schema).hash(foo1)
    val expect1 = combineHash(1.hashCode(), "intValue".hashCode())
    val expect2 = combineHash("foo".hashCode(), "stringValue".hashCode())
    expect.eql(expect1, hashOutput0) &&
    expect.eql(expect2, hashOutput1)
  }

  test("union with the same subtypes") {
    val foo0 = IntOrInt.IntValue0(1)
    val foo1 = IntOrInt.IntValue1(1)
    val hashOutput0 = visitor(IntOrInt.schema).hash(foo0)
    val hashOutput1 = visitor(IntOrInt.schema).hash(foo1)
    val expect1 = combineHash(1.hashCode(), "intValue0".hashCode())
    val expect2 = combineHash(1.hashCode(), "intValue1".hashCode())
    expect.eql(expect1, hashOutput0) &&
    expect.eql(expect2, hashOutput1)
  }

  test("default enum") {
    val foo = FooBar.Foo
    val hashOutput = visitor(FooBar.schema).hash(foo)
    val bar = FooBar.Bar
    val hashOutput1 = visitor(FooBar.schema).hash(bar)
    expect.eql(foo.stringValue.hashCode(), hashOutput)
    expect.eql(bar.stringValue.hashCode(), hashOutput1)
  }

  test("int enum") {
    val foo = IntFooBar.Foo
    val hashOutput = visitor(IntFooBar.schema).hash(foo)
    val bar = IntFooBar.Bar
    val hashOutput1 = visitor(IntFooBar.schema).hash(bar)
    expect.eql(foo.intValue.hashCode(), hashOutput)
    expect.eql(bar.intValue.hashCode(), hashOutput1)
  }

}
object HashTestUtils {
  implicit val recursiveOptionHash: Hash[RecursiveFoo] = {
    new Hash[RecursiveFoo] {
      override def hash(x: RecursiveFoo): Int =
        combineHash(
          productSeed,
          "RecursiveFoo".hashCode,
          Hash[Option[RecursiveFoo]].hash(x.foo)
        )
      override def eqv(x: RecursiveFoo, y: RecursiveFoo): Boolean =
        cats.Eq[RecursiveFoo].eqv(x, y)
    }
  }

  def testStructHash[A: Hash](name: String, a: A*): Int = {
    val nameHash = Hash[String].hash(name)
    val hashes = a.toList.map(Hash[A].hash)
    combineHash(productSeed, nameHash +: hashes: _*)
  }

}
