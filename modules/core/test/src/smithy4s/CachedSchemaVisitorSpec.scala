package smithy4s

import munit.FunSuite
import smithy4s.schema._

import java.util.concurrent.atomic.AtomicInteger

class CachedSchemaVisitorSpec() extends FunSuite {

  class TestSchemaVisitor(counter: AtomicInteger)
      extends CachedSchemaVisitor[Option] {

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
      counter.incrementAndGet()
      None
    }

    def map[K, V](
        shapeId: ShapeId,
        hints: Hints,
        key: Schema[K],
        value: Schema[V]
    ): Option[Map[K, V]] = {
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
      counter.incrementAndGet()
      None
    }

    def union[U](
        shapeId: ShapeId,
        hints: Hints,
        alternatives: Vector[SchemaAlt[U, _]],
        dispatch: Alt.Dispatcher[Schema, U]
    ): Option[U] = {
      counter.incrementAndGet()
      None
    }

    def biject[A, B](
        schema: Schema[A],
        bijection: Bijection[A, B]
    ): Option[B] = {
      counter.incrementAndGet()
      None
    }

    def refine[A, B](
        schema: Schema[A],
        refinement: Refinement[A, B]
    ): Option[B] = {
      counter.incrementAndGet()
      None
    }

    def lazily[A](suspend: Lazy[Schema[A]]): Option[A] = {
      counter.incrementAndGet()
      None
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
}
