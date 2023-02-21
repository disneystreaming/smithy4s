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
package schema

import Schema._

// format: off
sealed trait Schema[A]{
  def shapeId: ShapeId
  def hints: Hints
  final def required[Struct]: PartiallyAppliedRequired[Struct, A] = new PartiallyAppliedRequired[Struct, A](this)
  final def optional[Struct]: PartiallyAppliedOptional[Struct, A] = new PartiallyAppliedOptional[Struct, A](this)

  final def oneOf[Union]: PartiallyAppliedOneOf[Union, A] = new PartiallyAppliedOneOf[Union,A](this)

  final def compile[F[_]](fk: Schema ~> F): F[A] = fk(this)

  final def addHints(hints: Hint*): Schema[A] = transformHintsLocally(_ ++ Hints(hints:_*))
  final def addHints(hints: Hints): Schema[A] = transformHintsLocally(_ ++ hints)

  final def withId(newId: ShapeId): Schema[A] = this match {
    case PrimitiveSchema(_, hints, tag) => PrimitiveSchema(newId, hints, tag)
    case s: CollectionSchema[c, a] => CollectionSchema(newId, s.hints, s.tag, s.member).asInstanceOf[Schema[A]]
    case s: MapSchema[k, v] => MapSchema(newId, s.hints, s.key, s.value).asInstanceOf[Schema[A]]
    case EnumerationSchema(_, hints, values, total) => EnumerationSchema(newId, hints, values, total)
    case StructSchema(_, hints, fields, make) => StructSchema(newId, hints, fields, make)
    case UnionSchema(_, hints, alternatives, dispatch) => UnionSchema(newId, hints, alternatives, dispatch)
    case BijectionSchema(schema, bijection) => BijectionSchema(schema.withId(newId), bijection)
    case RefinementSchema(schema, refinement) => RefinementSchema(schema.withId(newId), refinement)
    case LazySchema(suspend) => LazySchema(suspend.map(_.withId(newId)))
  }

  final def withId(namespace: String, name: String): Schema[A] = withId(ShapeId(namespace, name))

  final def transformHintsLocally(f: Hints => Hints): Schema[A] = this match {
    case PrimitiveSchema(shapeId, hints, tag) => PrimitiveSchema(shapeId, f(hints), tag)
    case s: CollectionSchema[c, a] => CollectionSchema(s.shapeId, f(s.hints), s.tag, s.member).asInstanceOf[Schema[A]]
    case s: MapSchema[k, v] => MapSchema(s.shapeId, f(s.hints), s.key, s.value).asInstanceOf[Schema[A]]
    case EnumerationSchema(shapeId, hints, values, total) => EnumerationSchema(shapeId, f(hints), values, total)
    case StructSchema(shapeId, hints, fields, make) => StructSchema(shapeId, f(hints), fields, make)
    case UnionSchema(shapeId, hints, alternatives, dispatch) => UnionSchema(shapeId, f(hints), alternatives, dispatch)
    case BijectionSchema(schema, bijection) => BijectionSchema(schema.transformHintsLocally(f), bijection)
    case RefinementSchema(schema, refinement) => RefinementSchema(schema.transformHintsLocally(f), refinement)
    case LazySchema(suspend) => LazySchema(suspend.map(_.transformHintsLocally(f)))
  }

  final def transformHintsTransitively(f: Hints => Hints): Schema[A] = this match {
    case PrimitiveSchema(shapeId, hints, tag) => PrimitiveSchema(shapeId, f(hints), tag)
    case s: CollectionSchema[c, a] => CollectionSchema[c, a](s.shapeId, f(s.hints), s.tag, s.member.transformHintsTransitively(f)).asInstanceOf[Schema[A]]
    case s: MapSchema[k, v] => MapSchema(s.shapeId, f(s.hints), s.key.transformHintsTransitively(f), s.value.transformHintsTransitively(f)).asInstanceOf[Schema[A]]
    case EnumerationSchema(shapeId, hints, values, total) => EnumerationSchema(shapeId, f(hints), values.map(_.transformHints(f)), total andThen (_.transformHints(f)))
    case StructSchema(shapeId, hints, fields, make) => StructSchema(shapeId, f(hints), fields.map(_.mapK(Schema.transformHintsTransitivelyK(f))), make)
    case UnionSchema(shapeId, hints, alternatives, dispatch) => UnionSchema(shapeId, f(hints), alternatives.map(_.mapK(Schema.transformHintsTransitivelyK(f))), dispatch)
    case BijectionSchema(schema, bijection) => BijectionSchema(schema.transformHintsTransitively(f), bijection)
    case RefinementSchema(schema, refinement) => RefinementSchema(schema.transformHintsTransitively(f), refinement)
    case LazySchema(suspend) => LazySchema(suspend.map(_.transformHintsTransitively(f)))
  }

  final def validated[C](c: C)(implicit constraint: RefinementProvider.Simple[C, A]): Schema[A] = {
    val hint = Hints.Binding.fromValue(c)(constraint.tag)
    RefinementSchema(this.addHints(hint), constraint.make(c))
  }

  final def refined[B]: PartiallyAppliedRefinement[A, B] = new PartiallyAppliedRefinement[A, B](this)

  final def biject[B](to: A => B, from: B => A) : Schema[B] = Schema.bijection(this, to, from)

  final def getDefault: Option[Document] =
      this.hints.get(smithy.api.Default).map(_.value)

  final def getDefaultValue: Option[A] = getDefault.flatMap(Document.Decoder.fromSchema(this).decode(_).toOption)

  final def partial(filter: SchemaField[_, _] => Boolean): Wedge[Schema[PartialData[A]], Schema[A]] =
    smithy4s.internals.ToPartialSchema(filter, payload = false)(this)

  final def payloadPartial(find: SchemaField[_, _] => Boolean): Wedge[Schema[PartialData[A]], Schema[A]] =
    smithy4s.internals.ToPartialSchema(find, payload = true)(this)

}

object Schema {
  final case class PrimitiveSchema[P](shapeId: ShapeId, hints: Hints, tag: Primitive[P]) extends Schema[P]
  final case class CollectionSchema[C[_], A](shapeId: ShapeId, hints: Hints, tag: CollectionTag[C], member: Schema[A]) extends Schema[C[A]]
  final case class MapSchema[K, V](shapeId: ShapeId, hints: Hints, key: Schema[K], value: Schema[V]) extends Schema[Map[K, V]]
  final case class EnumerationSchema[E](shapeId: ShapeId, hints: Hints, values: List[EnumValue[E]], total: E => EnumValue[E]) extends Schema[E]
  final case class StructSchema[S](shapeId: ShapeId, hints: Hints, fields: Vector[SchemaField[S, _]], make: IndexedSeq[Any] => S) extends Schema[S]
  final case class UnionSchema[U](shapeId: ShapeId, hints: Hints, alternatives: Vector[SchemaAlt[U, _]], dispatch: U => Alt.SchemaAndValue[U, _]) extends Schema[U]
  final case class BijectionSchema[A, B](underlying: Schema[A], bijection: Bijection[A, B]) extends Schema[B]{
    def shapeId = underlying.shapeId
    def hints = underlying.hints
  }
  final case class RefinementSchema[A, B](underlying: Schema[A], refinement: Refinement[A, B]) extends Schema[B]{
    def shapeId = underlying.shapeId
    def hints = underlying.hints
  }
  final case class LazySchema[A](suspend: Lazy[Schema[A]]) extends Schema[A]{
    def shapeId: ShapeId = suspend.value.shapeId
    def hints: Hints = suspend.value.hints
  }

  def transformHintsLocallyK(f: Hints => Hints): Schema ~> Schema = new (Schema ~> Schema){
    def apply[A](fa: Schema[A]): Schema[A] = fa.transformHintsLocally(f)
  }

  def transformHintsTransitivelyK(f: Hints => Hints): Schema ~> Schema = new (Schema ~> Schema){
    def apply[A](fa: Schema[A]): Schema[A] = fa.transformHintsTransitively(f)
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // SCHEMA BUILDER
  //////////////////////////////////////////////////////////////////////////////////////////////////
  private val prelude = "smithy.api"

  val short: Schema[Short] = Primitive.PShort.schema(prelude, "Short")
  val int: Schema[Int] = Primitive.PInt.schema(prelude, "Integer")
  val long: Schema[Long] = Primitive.PLong.schema(prelude, "Long")
  val double: Schema[Double] = Primitive.PDouble.schema(prelude, "Double")
  val float: Schema[Float] = Primitive.PFloat.schema(prelude, "Float")
  val bigint: Schema[BigInt] = Primitive.PBigInt.schema(prelude, "BigInteger")
  val bigdecimal: Schema[BigDecimal] = Primitive.PBigDecimal.schema(prelude, "BigDecimal")
  val string: Schema[String] = Primitive.PString.schema(prelude, "String")
  val boolean: Schema[Boolean] = Primitive.PBoolean.schema(prelude, "Boolean")
  val byte: Schema[Byte] = Primitive.PByte.schema(prelude, "Byte")
  val bytes: Schema[ByteArray] = Primitive.PBlob.schema(prelude, "Blob")
  val unit: Schema[Unit] = Primitive.PUnit.schema(prelude, "Unit")
  val timestamp: Schema[Timestamp] = Primitive.PTimestamp.schema(prelude, "Timestamp")
  val document: Schema[Document] = Primitive.PDocument.schema(prelude, "Document")
  val uuid: Schema[java.util.UUID] = Primitive.PUUID.schema("alloy", "UUID")

  private val placeholder: ShapeId = ShapeId("placeholder", "Placeholder")

  def list[A](a: Schema[A]): Schema[List[A]] = Schema.CollectionSchema[List, A](placeholder, Hints.empty, CollectionTag.ListTag, a)
  def set[A](a: Schema[A]): Schema[Set[A]] = Schema.CollectionSchema[Set, A](placeholder, Hints.empty, CollectionTag.SetTag, a)
  def vector[A](a: Schema[A]): Schema[Vector[A]] = Schema.CollectionSchema[Vector, A](placeholder, Hints.empty, CollectionTag.VectorTag, a)
  def indexedSeq[A](a: Schema[A]): Schema[IndexedSeq[A]] = Schema.CollectionSchema[IndexedSeq, A](placeholder, Hints.empty, CollectionTag.IndexedSeqTag, a)

  def map[K, V](k: Schema[K], v: Schema[V]): Schema[Map[K, V]] = Schema.MapSchema(placeholder, Hints.empty, k, v)
  def recursive[A](s: => Schema[A]): Schema[A] = Schema.LazySchema(Lazy(s))

  def union[U](alts: SchemaAlt[U, _]*)(dispatch: U => Alt.SchemaAndValue[U, _]): Schema.UnionSchema[U] =
    Schema.UnionSchema(placeholder, Hints.empty, alts.toVector, dispatch)

  def union[U](alts: Vector[SchemaAlt[U, _]])(dispatch: U => Alt.SchemaAndValue[U, _]): Schema.UnionSchema[U] =
    Schema.UnionSchema(placeholder, Hints.empty, alts, dispatch)

  def enumeration[E](total: E => EnumValue[E], values: List[EnumValue[E]]): Schema[E] =
    Schema.EnumerationSchema(placeholder, Hints.empty, values, total)

  def enumeration[E <: Enumeration.Value](values: List[E]): Schema[E] =
    Schema.EnumerationSchema(placeholder, Hints.empty, values.map(Enumeration.Value.toSchema(_)), Enumeration.Value.toSchema[E])

  def bijection[A, B](a: Schema[A], bijection: Bijection[A, B]): Schema[B] =
    Schema.BijectionSchema(a, bijection)

  def bijection[A, B](a: Schema[A], to: A => B, from: B => A): Schema[B] =
    Schema.BijectionSchema(a, Bijection(to, from))

  def constant[A](a: A): Schema[A] = Schema.StructSchema(placeholder, Hints.empty, Vector.empty, _ => a)

  def struct[S]: PartiallyAppliedStruct[S] = new PartiallyAppliedStruct[S](placeholder)

  private [smithy4s] class PartiallyAppliedRequired[S, A](private val schema: Schema[A]) extends AnyVal {
    def apply(label: String, get: S => A): SchemaField[S, A] = Field.required(label, schema, get)
  }

  private [smithy4s] class PartiallyAppliedOptional[S, A](private val schema: Schema[A]) extends AnyVal {
    def apply(label: String, get: S => Option[A]): SchemaField[S, Option[A]] = Field.optional(label, schema, get)
  }

  private [smithy4s] class PartiallyAppliedOneOf[U, A](private val schema: Schema[A]) extends AnyVal {
    def apply(label: String)(implicit ev: A <:< U): SchemaAlt[U, A] = Alt(label, schema, ev)
    def apply(label: String, inject: A => U): SchemaAlt[U, A] = Alt(label, schema, inject)
  }

  private [smithy4s] class PartiallyAppliedRefinement[A, B](private val schema: Schema[A]) extends AnyVal {
    def apply[C](c: C)(implicit refinementProvider: RefinementProvider[C, A, B]): Schema[B] = {
      val hint = Hints.Binding.fromValue(c)(refinementProvider.tag)
      RefinementSchema(schema.addHints(hint), refinementProvider.make(c))
    }
  }
}
