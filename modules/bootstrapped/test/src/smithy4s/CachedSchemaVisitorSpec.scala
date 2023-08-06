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

import munit.FunSuite
import smithy4s.schema._
import smithy4s.schema.Schema._

import java.util.concurrent.atomic.AtomicInteger
import java.util.HashSet

class CachedSchemaVisitorSpec() extends FunSuite {

  type ConstUnit[A] = Unit
  def discard[A](a: => A): Unit = { val _ = a }

  // Counter is effectively counting Misses - meaning the SchemaVisitor was actually evaluated again
  class TestSchemaVisitor(
      counter: AtomicInteger,
      val cache: CompilationCache[ConstUnit]
  ) extends SchemaVisitor.Cached[ConstUnit] { self =>

    private val lazyTracker: HashSet[ShapeId] = new HashSet()

    def primitive[P](
        shapeId: ShapeId,
        hints: Hints,
        tag: Primitive[P]
    ): Unit = discard {
      counter.incrementAndGet()
    }

    def collection[C[_], A](
        shapeId: ShapeId,
        hints: Hints,
        tag: CollectionTag[C],
        member: Schema[A]
    ): Unit = discard {
      self(member)
      counter.incrementAndGet()
    }

    def map[K, V](
        shapeId: ShapeId,
        hints: Hints,
        key: Schema[K],
        value: Schema[V]
    ): Unit = discard {
      self(key)
      self(value)
      counter.incrementAndGet()
    }

    def enumeration[E](
        shapeId: ShapeId,
        hints: Hints,
        tag: EnumTag,
        values: List[EnumValue[E]],
        total: E => EnumValue[E]
    ): Unit = discard {
      counter.incrementAndGet()
    }

    def struct[S](
        shapeId: ShapeId,
        hints: Hints,
        fields: Vector[Field[S, _]],
        make: IndexedSeq[Any] => S
    ): Unit = discard {
      fields.foreach { field =>
        self(field.instance)
      }
      counter.incrementAndGet()
    }

    def union[U](
        shapeId: ShapeId,
        hints: Hints,
        alternatives: Vector[Alt[U, _]],
        dispatch: Alt.Dispatcher[U]
    ): Unit = discard {
      alternatives.foreach { alt =>
        self(alt.schema)
      }
      counter.incrementAndGet()
    }

    def biject[A, B](
        schema: Schema[A],
        bijection: Bijection[A, B]
    ): Unit = discard {
      self(schema)
      counter.incrementAndGet()
    }

    def refine[A, B](
        schema: Schema[A],
        refinement: Refinement[A, B]
    ): Unit = discard {
      self(schema)
      counter.incrementAndGet()
    }

    def lazily[A](suspend: Lazy[Schema[A]]): Unit = discard {
      val shapeId = suspend.value.shapeId
      if (!lazyTracker.contains(suspend.value.shapeId)) {
        lazyTracker.add(shapeId)
        counter.incrementAndGet()
        self(suspend.value)
        lazyTracker.remove(shapeId)
      }
    }

    def option[A](schema: Schema[A]): ConstUnit[Option[A]] = discard {
      self(schema)
      counter.incrementAndGet()
    }
  }

  def checkSchema[A](schema: Schema[A]): Unit = {
    val cache = CompilationCache.make[ConstUnit]
    val counter = new AtomicInteger(0)
    val visitor = new TestSchemaVisitor(counter, cache)
    val schema = Schema.int
    visitor(schema)
    val before = counter.get()
    visitor(schema)
    val after = counter.get()
    assertEquals(before, after)
  }

  val header = "Cache should prevent recomputation: "

  test(header + "primitive") {
    checkSchema(int)
  }

  test(header + "collection") {
    checkSchema(list(int))
  }

  test(header + "map") {
    checkSchema(map(string, int))
  }

  test(header + "enum") {
    sealed abstract class FooBar(val stringValue: String, val intValue: Int)
        extends smithy4s.Enumeration.Value {
      override type EnumType = FooBar
      val name = stringValue
      val value = stringValue
      val hints = Hints.empty
      def enumeration: smithy4s.Enumeration[EnumType] = FooBar
    }

    object FooBar extends smithy4s.Enumeration[FooBar] {
      def hints = Hints.empty
      def id = ShapeId("", "FooBar")
      def values: List[FooBar] = List(Foo, Bar)

      case object Foo extends FooBar("foo", 0)
      case object Bar extends FooBar("bar", 1)

      implicit val schema: Schema[FooBar] =
        stringEnumeration[FooBar](List(Foo, Bar))
    }

    val schema: Schema[FooBar] = stringEnumeration[FooBar](FooBar.values)
    checkSchema(schema)
  }

  test(header + "struct") {
    case class Foo(int: Int)
    val schema = struct(int.required[Foo]("int", _.int))(Foo(_))
    checkSchema(schema)
  }

  test(header + "union") {
    val schema = Schema.either(int, string)
    checkSchema(schema)
  }

  test((header + "lazy")) {
    case class Foo(foo: Option[Foo])
    object Foo {
      val schema: Schema[Foo] = recursive {
        val foos = schema.optional[Foo]("foo", _.foo)
        struct(foos)(Foo.apply)
      }
    }
    checkSchema(Foo.schema)
  }

  test(header + "bijection") {
    case class Foo(x: Int)
    val schema: Schema[Foo] = bijection(int, Foo(_), _.x)
    checkSchema(schema)
  }

  test(header + "surjection") {
    val schema: Schema[Int] =
      int.refined(smithy.api.Range(None, Option(BigDecimal(1))))
    checkSchema(schema)
  }
}
