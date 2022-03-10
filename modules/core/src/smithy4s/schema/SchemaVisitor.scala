package smithy4s
package schema

import Schema._

// format: off
trait SchemaVisitor[F[_]] extends (Schema ~> F){
  def primitive[P](shapeId: ShapeId, hints: Hints, tag: Primitive[P]) : F[P]
  def list[A](shapeId: ShapeId, hints: Hints, member: Schema[A]) : F[List[A]]
  def set[A](shapeId: ShapeId, hints: Hints, member: Schema[A]): F[Set[A]]
  def map[K, V](shapeId: ShapeId, hints: Hints, key: Schema[K], value: Schema[V]): F[Map[K, V]]
  def enumeration[E](shapeId: ShapeId, hints: Hints, values: List[EnumValue[E]], total: E => EnumValue[E]) : F[E]
  def struct[S](shapeId: ShapeId, hints: Hints, fields: Vector[SchemaField[S, _]], make: IndexedSeq[Any] => S) : F[S]
  def union[U](shapeId: ShapeId, hints: Hints, alternatives: Vector[SchemaAlt[U, _]], dispatch: U => Alt.SchemaAndValue[U, _]) : F[U]
  def biject[A, B](schema: Schema[A], to: A => B, from: B => A) : F[B]
  def surject[A, B](schema: Schema[A], to: A => Either[Throwable, B], from: B  => A) : F[B]
  def lazily[A](suspend: Lazy[Schema[A]]) : F[A]

  def apply[A](schema: Schema[A]) : F[A] = schema match {
    case PrimitiveSchema(shapeId, hints, tag) => primitive(shapeId, hints, tag)
    case ListSchema(shapeId, hints, member) => list(shapeId, hints, member)
    case SetSchema(shapeId, hints, member) => set(shapeId, hints, member)
    case MapSchema(shapeId, hints, key, value) => map(shapeId, hints, key, value)
    case EnumerationSchema(shapeId, hints, values, total) => enumeration(shapeId, hints, values, total)
    case StructSchema(shapeId, hints, fields, make) => struct(shapeId, hints, fields, make)
    case UnionSchema(shapeId, hints, alts, dispatch) => union(shapeId, hints, alts, dispatch)
    case BijectionSchema(schema, to, from) => biject(schema, to, from)
    case LazySchema(make) => lazily(make)
  }
}
