package smithy4s.compliancetests.internals

import smithy.api._
import smithy4s.schema.{
  Alt,
  CollectionTag,
  EnumValue,
  Primitive,
  Schema,
  SchemaAlt,
  SchemaField,
  SchemaVisitor
}
import smithy4s.{kinds, Bijection, HintMask, Hints, Lazy, Refinement, ShapeId}
import smithy4s.compliancetests.internals.MappedHintsSchemaVisitor.hintMask
import smithy4s.schema.Schema._

object MappedHintsSchemaVisitor {
  def apply(func: Hints => Hints): MappedHintsSchemaVisitor =
    new MappedHintsSchemaVisitor(func)

  private[compliancetests] val hintMask =
    HintMask(HttpLabel, HttpHeader, HttpQuery, HttpQueryParams, TimestampFormat)
}
class MappedHintsSchemaVisitor(func: Hints => Hints)
    extends SchemaVisitor[Schema] { self =>
  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): Schema[P] = {
    PrimitiveSchema(shapeId, func(hints), tag)
  }

  override def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Schema[A]
  ): Schema[C[A]] = {
    val memberMapped: Schema[A] = self(member.addHints(hintMask(hints)))
    CollectionSchema(shapeId, func(hints), tag, memberMapped)
  }

  override def map[K, V](
      shapeId: ShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ): Schema[Map[K, V]] = {
    val keyMapped: Schema[K] = self(key.addHints(hintMask(hints)))
    val valueMapped: Schema[V] = self(value.addHints(hintMask(hints)))
    MapSchema(shapeId, func(hints), keyMapped, valueMapped)
  }

  override def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): Schema[E] =
    EnumerationSchema(shapeId, func(hints), values, total)

  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[SchemaField[S, _]],
      make: IndexedSeq[Any] => S
  ): Schema[S] = {
    val addHintsTransformation = transformK(hints)
    val mappedFields: Vector[SchemaField[S, _]] = fields.map { field =>
      field.mapK(addHintsTransformation)
    }

    StructSchema(shapeId, func(hints), mappedFields, make)
  }

  private def transformK(hints: Hints) =
    new kinds.PolyFunction[Schema, Schema] {
      override def apply[A](fa: Schema[A]): Schema[A] = self(
        fa.addHints(hintMask(hints))
      )
    }

  override def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[SchemaAlt[U, _]],
      dispatch: Alt.Dispatcher[Schema, U]
  ): Schema[U] = {
    val transformedAlts: Vector[SchemaAlt[U, _]] = alternatives.map { alt =>
      alt.mapK(transformK(hints))
    }
    def project[A](
        alt: SchemaAlt[U, A]
    ): U => Option[Alt.SchemaAndValue[U, A]] = { (u: U) =>
      dispatch.projector(alt)(u).map(alt(_))
    }
    val projectedAlts: Vector[U => Option[Alt.SchemaAndValue[U, _]]] =
      transformedAlts.map(schemaAlt => project(schemaAlt))

    val reduced: U => Option[Alt.SchemaAndValue[U, _]] =
      projectedAlts.reduce((a, b) => u => a(u).orElse(b(u)))

    UnionSchema(shapeId, func(hints), transformedAlts, reduced(_).get)
  }

  override def biject[A, B](
      schema: Schema[A],
      bijection: Bijection[A, B]
  ): Schema[B] = {
    val mapped = self(schema)
    BijectionSchema(mapped, bijection)
  }

  override def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ): Schema[B] = {
    val mapped = self(schema)
    RefinementSchema(mapped, refinement)
  }

  override def lazily[A](suspend: Lazy[Schema[A]]): Schema[A] = {
    val mapped = suspend.map(self(_))
    LazySchema(mapped)
  }
}
