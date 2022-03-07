package smithy4s
package schema

import Schema._

sealed trait Schema[A]{
  def shapeId: ShapeId
  def hints: Hints
  def required[Struct](label: String, get: Struct => A, hints: Hint*): SchemaField[Struct, A] = Field.required(label, this, get, hints: _*)
  def optional[Struct](label: String, get: Struct => Option[A], hints: Hint*): SchemaField[Struct, Option[A]] = Field.optional(label, this, get, hints: _*)
  def oneOf[Union](label: String)(implicit ev: A <:< Union): SchemaAlt[Union, A] = Alt(label, this, ev)

  def compile[F[_]](fk : Schema ~> F) : F[A] = fk(this)
  def transformHints(f: Hints => Hints) : Schema[A] = this match {
    case PrimitiveSchema(shapeId, hints, tag) => PrimitiveSchema(shapeId, f(hints), tag)
    case s: ListSchema[a] => ListSchema(s.shapeId, f(s.hints), s.member).asInstanceOf[Schema[A]]
    case s: SetSchema[a] => SetSchema(s.shapeId, f(s.hints), s.member).asInstanceOf[Schema[A]]
    case s: MapSchema[k, v] => MapSchema(s.shapeId, f(s.hints), s.key, s.value).asInstanceOf[Schema[A]]
    case EnumerationSchema(shapeId, hints, values, total) => EnumerationSchema(shapeId, f(hints), values, total)
    case StructSchema(shapeId, hints, fields, make) => StructSchema(shapeId, f(hints), fields, make)
    case UnionSchema(shapeId, hints, alternatives, dispatch) => UnionSchema(shapeId, hints, alternatives, dispatch)
    case BijectionSchema(schema, to, from) => BijectionSchema(schema.transformHints(f), to, from)
    case LazySchema(suspend) => LazySchema(suspend.map(_.transformHints(f)))
  }
}

object Schema {
  case class PrimitiveSchema[P](shapeId: ShapeId, hints: Hints, tag: Primitive.Aux[P]) extends Schema[P]
  case class ListSchema[A](shapeId: ShapeId, hints: Hints, member: Schema[A]) extends Schema[List[A]]
  case class SetSchema[A](shapeId: ShapeId, hints: Hints, member: Schema[A]) extends Schema[Set[A]]
  case class MapSchema[K, V](shapeId: ShapeId, hints: Hints, key: Schema[K], value: Schema[V]) extends Schema[Map[K, V]]
  case class EnumerationSchema[E](shapeId: ShapeId, hints: Hints, values: List[EnumValue[E]], total: E => EnumValue[E]) extends Schema[E]
  case class StructSchema[S](shapeId: ShapeId, hints: Hints, fields: Vector[SchemaField[S, _]], make: IndexedSeq[Any] => S) extends Schema[S]
  case class UnionSchema[U](shapeId: ShapeId, hints: Hints, alternatives: Vector[SchemaAlt[U, _]], dispatch: U => Alt.SchemaAndValue[U, _]) extends Schema[U]
  case class BijectionSchema[A, B](underlying: Schema[A], to: A => B, from: B => A) extends Schema[B]{
    def shapeId = underlying.shapeId
    def hints = underlying.hints
  }
  case class LazySchema[A](suspend : Lazy[Schema[A]]) extends Schema[A]{
    def shapeId: ShapeId = suspend.value.shapeId
    def hints: Hints = suspend.value.hints
  }
}
