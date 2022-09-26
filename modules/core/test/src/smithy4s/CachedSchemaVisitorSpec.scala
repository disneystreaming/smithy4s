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

class CachedSchemaVisitorSpec() extends FunSuite {

  // Counter is effectively counting Misses - meaning the SchemaVisitor was actually evaluated again
  class TestSchemaVisitor(counter: AtomicInteger)
      extends SchemaVisitor.Cached[Option] { self =>

    def primitive[P](
        shapeId: ShapeId,
        hints: Hints,
        tag: Primitive[P]
    ): Option[P] = {
      counter.incrementAndGet()
      None
    }

    def collection[C[_], A](
        shapeId: ShapeId,
        hints: Hints,
        tag: CollectionTag[C],
        member: Schema[A]
    ): Option[C[A]] = {
      self(member)
      counter.incrementAndGet()
      None
    }

    def map[K, V](
        shapeId: ShapeId,
        hints: Hints,
        key: Schema[K],
        value: Schema[V]
    ): Option[Map[K, V]] = {
      self(key)
      self(value)
      counter.incrementAndGet()
      None
    }

    def enumeration[E](
        shapeId: ShapeId,
        hints: Hints,
        values: List[EnumValue[E]],
        total: E => EnumValue[E]
    ): Option[E] = {
      counter.incrementAndGet()
      None
    }

    def struct[S](
        shapeId: ShapeId,
        hints: Hints,
        fields: Vector[SchemaField[S, _]],
        make: IndexedSeq[Any] => S
    ): Option[S] = {
      fields.foreach { field =>
        self(field.instance)
      }
      counter.incrementAndGet()
      None
    }

    def union[U](
        shapeId: ShapeId,
        hints: Hints,
        alternatives: Vector[SchemaAlt[U, _]],
        dispatch: Alt.Dispatcher[Schema, U]
    ): Option[U] = {
      alternatives.foreach { alt =>
        self(alt.instance)
      }
      counter.incrementAndGet()
      None
    }

    def biject[A, B](
        schema: Schema[A],
        bijection: Bijection[A, B]
    ): Option[B] = {
      self(schema)
      counter.incrementAndGet()
      None
    }

    def refine[A, B](
        schema: Schema[A],
        refinement: Refinement[A, B]
    ): Option[B] = {
      self(schema)
      counter.incrementAndGet()
      None
    }

    def lazily[A](suspend: Lazy[Schema[A]]): Option[A] = {
      counter.incrementAndGet()
      self(suspend.value)
    }
  }

  test("Sanity check should NOT hit cache on first visit") {
    val counter = new AtomicInteger(0)
    val visitor = new TestSchemaVisitor(counter)
    val schema = Schema.int
    val result: Option[Int] = visitor(schema)
    assertEquals(result, None)
    assertEquals(counter.get(), 1)
  }

  test(
    "should hit the cache on second visit with the same schema resulting in counter = 1"
  ) {
    val counter = new AtomicInteger(0)
    val visitor = new TestSchemaVisitor(counter)
    val schema = Schema.int
    val result: Option[Int] = visitor(schema)
    val _ = visitor(schema)
    assertEquals(result, None)
    assertEquals(counter.get(), 1)
  }

  test(
    "should miss cache on second visit with different schema and the same ShapeId"
  ) {
    val counter = new AtomicInteger(0)
    val visitor = new TestSchemaVisitor(counter)
    val schema = Schema.unit
    val lazySchema = Schema.recursive(schema)
    val result = visitor(schema)
    val _ = visitor(lazySchema)
    assertEquals(result, None)
    assertEquals(counter.get(), 2)
  }

  test(
    "test struct with multiple schemas "
  ) {
    case class Foo(i: Int, s: String, b: Boolean)
    val counter = new AtomicInteger(0)
    val visitor = new TestSchemaVisitor(counter)
    val schema: Schema[Foo] = struct(
      int.required[Foo]("i", _.i),
      string.required[Foo]("s", _.s),
      boolean.required[Foo]("b", _.b)
    ) {
      Foo.apply
    }
    val lazySchema = Schema.recursive(schema)
    val result = visitor(schema)
    val _ = visitor(lazySchema)
    assertEquals(result, None)
    assertEquals(counter.get(), 5)
  }
}
