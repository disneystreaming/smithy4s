package smithy4s.schema

import smithy4s.Lazy

import smithy4s.Bijection
import smithy4s.Refinement

import smithy4s.{Hints, ShapeId}

private[schema] class HashVisitor() extends SchemaVisitor[Lambda[A => Unit]] {
  private var result: Int = 1
  private val prime: Int = 31
  private val visitedLazy: java.util.HashSet[ShapeId] =
    new java.util.HashSet[ShapeId]()

  def getResult: Int = result

  def primitive[P](shapeId: ShapeId, hints: Hints, tag: Primitive[P]): Unit = {
    result = result * prime + 1
    result = result * prime + shapeId.hashCode()
    result = result * prime + hints.hashCode()
    result = result * prime + tag.hashCode()
  }

  def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Schema[A]
  ): Unit = {
    result = result * prime + 2
    result = result * prime + shapeId.hashCode()
    result = result * prime + hints.hashCode()
    result = result * prime + tag.hashCode()
  }

  def map[K, V](
      shapeId: ShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ): Unit = {
    result = result * prime + 3
    result = result * prime + shapeId.hashCode()
    result = result * prime + hints.hashCode()
    this(key)
    this(value)
  }

  def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): Unit = {
    result = result * prime + 4
    result = result * prime + shapeId.hashCode()
    result = result * prime + hints.hashCode()
    for (v <- values) {
      result = result * prime + v.hashCode()
    }
  }

  def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[SchemaField[S, _]],
      make: IndexedSeq[Any] => S
  ): Unit = {
    result = result * prime + 5
    result = result * prime + shapeId.hashCode()
    result = result * prime + hints.hashCode()
    def processField(field: SchemaField[S, _]): Unit = {
      result = result * prime + field.label.hashCode()
      this(field.instance)
    }
    fields.foreach(processField(_))
  }

  def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[SchemaAlt[U, _]],
      dispatch: Alt.Dispatcher[Schema, U]
  ): Unit = {
    result = result * prime + 6
    result = result * prime + shapeId.hashCode()
    result = result * prime + hints.hashCode()
    def processAlt(field: SchemaAlt[U, _]): Unit = {
      result = result * prime + field.label.hashCode()
      this(field.instance)
    }
    alternatives.foreach(processAlt(_))
  }

  def biject[A, B](schema: Schema[A], bijection: Bijection[A, B]): Unit = {
    result = result * prime + 7
    this(schema)
  }

  def refine[A, B](schema: Schema[A], refinement: Refinement[A, B]): Unit = {
    result = result * prime + 8
    this(schema)
  }

  def lazily[A](suspend: Lazy[Schema[A]]): Unit = {
    result = result * prime + 9
    val shapeId = suspend.value.shapeId
    if (!visitedLazy.contains(suspend.value.shapeId)) {
      visitedLazy.add(shapeId)
      this(suspend.value)
    }
  }

}
