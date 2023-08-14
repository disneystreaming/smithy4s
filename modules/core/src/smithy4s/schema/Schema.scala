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
import scala.reflect.ClassTag

// format: off
sealed trait Schema[A]{
  def shapeId: ShapeId
  def hints: Hints
  final def required[Struct]: PartiallyAppliedField[Struct, A] = new PartiallyAppliedField[Struct, A](this.addHints(smithy.api.Required()))
  final def optional[Struct]: PartiallyAppliedField[Struct, Option[A]] = new PartiallyAppliedField[Struct, Option[A]](this.option)
  final def removable[Struct]: PartiallyAppliedField[Struct, Removable[A]] = new PartiallyAppliedField[Struct, Removable[A]](Removable.schema(this))

  final def oneOf[Union]: PartiallyAppliedOneOf[Union, A] = new PartiallyAppliedOneOf[Union,A](this)

  final def compile[F[_]](fk: Schema ~> F): F[A] = fk(this)

  final def addHints(hints: Hint*): Schema[A] = transformHintsLocally(_ ++ Hints(hints:_*))
  final def addMemberHints(hints: Hint*): Schema[A] = transformHintsLocally(_.addMemberHints(Hints(hints:_*)))

  final def addHints(hints: Hints): Schema[A] = transformHintsLocally(_ ++ hints)
  final def addMemberHints(hints: Hints): Schema[A] = transformHintsLocally(_.addMemberHints(hints))

  final def withId(newId: ShapeId): Schema[A] = this match {
    case PrimitiveSchema(_, hints, tag) => PrimitiveSchema(newId, hints, tag)
    case s: CollectionSchema[c, a] => CollectionSchema(newId, s.hints, s.tag, s.member).asInstanceOf[Schema[A]]
    case s: MapSchema[k, v] => MapSchema(newId, s.hints, s.key, s.value).asInstanceOf[Schema[A]]
    case EnumerationSchema(_, hints, values, tag, total) => EnumerationSchema(newId, hints, values, tag, total)
    case StructSchema(_, hints, fields, make) => StructSchema(newId, hints, fields, make)
    case UnionSchema(_, hints, alternatives, dispatch) => UnionSchema(newId, hints, alternatives, dispatch)
    case BijectionSchema(schema, bijection) => BijectionSchema(schema.withId(newId), bijection)
    case RefinementSchema(schema, refinement) => RefinementSchema(schema.withId(newId), refinement)
    case LazySchema(suspend) => LazySchema(suspend.map(_.withId(newId)))
    case s: OptionSchema[a] => OptionSchema(s.underlying.withId(newId)).asInstanceOf[Schema[A]]
  }

  final def withId(namespace: String, name: String): Schema[A] = withId(ShapeId(namespace, name))

  final def transformHintsLocally(f: Hints => Hints): Schema[A] = this match {
    case PrimitiveSchema(shapeId, hints, tag) => PrimitiveSchema(shapeId, f(hints), tag)
    case s: CollectionSchema[c, a] => CollectionSchema(s.shapeId, f(s.hints), s.tag, s.member).asInstanceOf[Schema[A]]
    case s: MapSchema[k, v] => MapSchema(s.shapeId, f(s.hints), s.key, s.value).asInstanceOf[Schema[A]]
    case EnumerationSchema(shapeId, hints, values, tag, total) => EnumerationSchema(shapeId, f(hints), values, tag, total)
    case StructSchema(shapeId, hints, fields, make) => StructSchema(shapeId, f(hints), fields, make)
    case UnionSchema(shapeId, hints, alternatives, dispatch) => UnionSchema(shapeId, f(hints), alternatives, dispatch)
    case BijectionSchema(schema, bijection) => BijectionSchema(schema.transformHintsLocally(f), bijection)
    case RefinementSchema(schema, refinement) => RefinementSchema(schema.transformHintsLocally(f), refinement)
    case LazySchema(suspend) => LazySchema(suspend.map(_.transformHintsLocally(f)))
    case s: OptionSchema[a] => OptionSchema(s.underlying.transformHintsLocally(f)).asInstanceOf[Schema[A]]
  }

  final def transformHintsTransitively(f: Hints => Hints): Schema[A] = this match {
    case PrimitiveSchema(shapeId, hints, tag) => PrimitiveSchema(shapeId, f(hints), tag)
    case s: CollectionSchema[c, a] => CollectionSchema[c, a](s.shapeId, f(s.hints), s.tag, s.member.transformHintsTransitively(f)).asInstanceOf[Schema[A]]
    case s: MapSchema[k, v] => MapSchema(s.shapeId, f(s.hints), s.key.transformHintsTransitively(f), s.value.transformHintsTransitively(f)).asInstanceOf[Schema[A]]
    case EnumerationSchema(shapeId, hints, tag, values, total) => EnumerationSchema(shapeId, f(hints), tag, values.map(_.transformHints(f)), total andThen (_.transformHints(f)))
    case StructSchema(shapeId, hints, fields, make) => StructSchema(shapeId, f(hints), fields.map(_.transformHintsTransitively(f)), make)
    case UnionSchema(shapeId, hints, alternatives, dispatch) => UnionSchema(shapeId, f(hints), alternatives.map(_.transformHintsTransitively(f)), dispatch)
    case BijectionSchema(schema, bijection) => BijectionSchema(schema.transformHintsTransitively(f), bijection)
    case RefinementSchema(schema, refinement) => RefinementSchema(schema.transformHintsTransitively(f), refinement)
    case LazySchema(suspend) => LazySchema(suspend.map(_.transformHintsTransitively(f)))
    case s: OptionSchema[a] => OptionSchema(s.underlying.transformHintsTransitively(f)).asInstanceOf[Schema[A]]
  }

  final def validated[C](c: C)(implicit constraint: RefinementProvider.Simple[C, A]): Schema[A] = {
    val hint = Hints.Binding.fromValue(c)(constraint.tag)
    RefinementSchema(this.addHints(hint), constraint.make(c))
  }

  final def refined[B]: PartiallyAppliedRefinement[A, B] = new PartiallyAppliedRefinement[A, B](this)

  final def biject[B](bijection: Bijection[A, B]) : Schema[B] = Schema.bijection(this, bijection)
  final def biject[B](to: A => B, from: B => A) : Schema[B] = Schema.bijection(this, to, from)
  final def option: Schema[Option[A]] = Schema.option(this)

  final def isOption: Boolean = this match {
    case _: OptionSchema[_] => true
    case _ => false
  }

  final def getDefault: Option[Document] =
    this.hints.get(smithy.api.Default).map(_.value)

  final def getDefaultValue: Option[A] = {
    val maybeDefault = getDefault.flatMap[A] {
      case Document.DNull => this.compile(DefaultValueSchemaVisitor)
      case document => Document.Decoder.fromSchema(this).decode(document).toOption
    }
    maybeDefault.orElse(this.compile(OptionDefaultVisitor))
  }


  /**
    * When applied on a structure schema, creates a schema that, when compiled into
    * a codec, will only encode/decode a subset of the data, based on the hints
    * of each field.
    *
    * This can be used to only encode some fields of the data into the http body
    *
    * Returns a SchemaPartition that indicates whether :
    *   * no field match the condition
    *   * some fields match the condition
    *   * all fields match the condition
    */
  final def partition(filter: Field[_, _] => Boolean): SchemaPartition[A] =
    SchemaPartition(filter, payload = false)(this)

  /**
    * Finds the first field that matches the criteria used, and applies a bijection
    * between the schema it holds and partial data, which ensures for the field's schema to
    * be used as "top level" when decoding "payloads".
    *
    * NB : a "payload" is typically a whole set of data, without a typical field-based splitting
    * into subparts. This can be, for instance, an http body.
    */
  final def findPayload(find: Field[_, _] => Boolean): SchemaPartition[A] =
    SchemaPartition(find, payload = true)(this)

  /**
    * Finds whether a schema (or the underlying schema in the case of bijections/surjections, etc)
    * is a primitive of a certain type.
    */
  final def isPrimitive[P](prim: Primitive[P]) : Boolean = IsPrimitive(this, prim)

  /**
    * Checks whether a schema is Unit or an empty structure
    */
  final def isUnit: Boolean = this.shapeId == ShapeId("smithy.api", "Unit")

}

object Schema {

  def apply[A](implicit ev: Schema[A]): ev.type = ev

  final case class PrimitiveSchema[P](shapeId: ShapeId, hints: Hints, tag: Primitive[P]) extends Schema[P]
  final case class CollectionSchema[C[_], A](shapeId: ShapeId, hints: Hints, tag: CollectionTag[C], member: Schema[A]) extends Schema[C[A]]
  final case class MapSchema[K, V](shapeId: ShapeId, hints: Hints, key: Schema[K], value: Schema[V]) extends Schema[Map[K, V]]
  final case class EnumerationSchema[E](shapeId: ShapeId, hints: Hints, tag: EnumTag, values: List[EnumValue[E]], total: E => EnumValue[E]) extends Schema[E]
  final case class StructSchema[S](shapeId: ShapeId, hints: Hints, fields: Vector[Field[S, _]], make: IndexedSeq[Any] => S) extends Schema[S]
  final case class UnionSchema[U](shapeId: ShapeId, hints: Hints, alternatives: Vector[Alt[U, _]], ordinal: U => Int) extends Schema[U]
  final case class OptionSchema[A](underlying: Schema[A]) extends Schema[Option[A]]{
    def hints: Hints = underlying.hints
    def shapeId: ShapeId = underlying.shapeId
  }
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
  val bytes: Schema[Blob] = Primitive.PBlob.schema(prelude, "Blob")
  val blob: Schema[Blob] = Primitive.PBlob.schema(prelude, "Blob")
  val timestamp: Schema[Timestamp] = Primitive.PTimestamp.schema(prelude, "Timestamp")
  val document: Schema[Document] = Primitive.PDocument.schema(prelude, "Document")
  val uuid: Schema[java.util.UUID] = Primitive.PUUID.schema("alloy", "UUID")

  val unit: Schema[Unit] = Schema.StructSchema(ShapeId("smithy.api", "Unit"), Hints.empty, Vector.empty, _ => ())

  private[schema] val placeholder: ShapeId = ShapeId("placeholder", "Placeholder")

  def list[A](a: Schema[A]): Schema[List[A]] = Schema.CollectionSchema[List, A](placeholder, Hints.empty, CollectionTag.ListTag, a)
  def set[A](a: Schema[A]): Schema[Set[A]] = Schema.CollectionSchema[Set, A](placeholder, Hints.empty, CollectionTag.SetTag, a)
  def vector[A](a: Schema[A]): Schema[Vector[A]] = Schema.CollectionSchema[Vector, A](placeholder, Hints.empty, CollectionTag.VectorTag, a)
  def indexedSeq[A](a: Schema[A]): Schema[IndexedSeq[A]] = Schema.CollectionSchema[IndexedSeq, A](placeholder, Hints.empty, CollectionTag.IndexedSeqTag, a)

  def sparseList[A](a: Schema[A]): Schema[List[Option[A]]] = list(option(a))
  def sparseSet[A](a: Schema[A]): Schema[Set[Option[A]]] = set(option(a))
  def sparseVector[A](a: Schema[A]): Schema[Vector[Option[A]]] = vector(option(a))
  def sparseIndexedSeq[A](a: Schema[A]): Schema[IndexedSeq[Option[A]]] = indexedSeq(option(a))

  def map[K, V](k: Schema[K], v: Schema[V]): Schema[Map[K, V]] = Schema.MapSchema(placeholder, Hints.empty, k, v)
  def sparseMap[K, V](k: Schema[K], v: Schema[V]): Schema[Map[K, Option[V]]] = Schema.MapSchema(placeholder, Hints.empty, k, option(v))

  def option[A](s: Schema[A]): Schema[Option[A]] = Schema.OptionSchema(s)

  def recursive[A](s: => Schema[A]): Schema[A] = Schema.LazySchema(Lazy(s))

  def union[U](alts: Vector[Alt[U, _]]): PartiallyAppliedUnion[U] = new PartiallyAppliedUnion(alts)
  def union[U](alts: Alt[U, _]*) : PartiallyAppliedUnion[U] = new PartiallyAppliedUnion(alts.toVector)

  def either[A, B](left: Schema[A], right: Schema[B]) : Schema[Either[A, B]] = {
    val l = left.oneOf[Either[A, B]]("left", Left(_: A)) { case Left(a) => a }
    val r = right.oneOf[Either[A, B]]("right", Right(_: B)) { case Right(b) => b }
    union(l, r) {
      case Left(_) => 0
      case Right(_) =>  1
    }
  }

  def enumeration[E](total: E => EnumValue[E], tag: EnumTag, values: List[EnumValue[E]]): Schema[E] =
    Schema.EnumerationSchema(placeholder, Hints.empty, tag, values, total)

  def stringEnumeration[E](total: E => EnumValue[E], values: List[EnumValue[E]]): Schema[E] =
    enumeration(total, EnumTag.StringEnum, values)

  def intEnumeration[E](total: E => EnumValue[E], values: List[EnumValue[E]]): Schema[E] =
    enumeration(total, EnumTag.IntEnum, values)

  def enumeration[E <: Enumeration.Value](tag: EnumTag, values: List[E]): Schema[E] =
    Schema.EnumerationSchema(placeholder, Hints.empty, tag, values.map(Enumeration.Value.toSchema(_)), Enumeration.Value.toSchema[E])

  def stringEnumeration[E <: Enumeration.Value](values: List[E]): Schema[E] =
    enumeration(EnumTag.StringEnum, values)

  def intEnumeration[E <: Enumeration.Value](values: List[E]): Schema[E] =
    enumeration(EnumTag.IntEnum, values)

  def bijection[A, B](a: Schema[A], bijection: Bijection[A, B]): Schema[B] =
    Schema.BijectionSchema(a, bijection)

  def bijection[A, B](a: Schema[A], to: A => B, from: B => A): Schema[B] =
    Schema.BijectionSchema(a, Bijection(to, from))

  def constant[A](a: A): Schema[A] = Schema.StructSchema(placeholder, Hints.empty, Vector.empty, _ => a)

  def struct[S]: PartiallyAppliedStruct[S] = new PartiallyAppliedStruct[S](placeholder)
  val tuple: PartiallyAppliedTuple = new PartiallyAppliedTuple(placeholder)
  private [smithy4s] class PartiallyAppliedField[S, A](private val schema: Schema[A]) extends AnyVal {
    def apply(label: String, get: S => A): Field[S, A] = Field(label, schema, get)
  }

  private [smithy4s] class PartiallyAppliedOptional[S, A](private val schema: Schema[A]) extends AnyVal {
    def apply(label: String, get: S => Option[A]): Field[S, Option[A]] = Field.optional(label, schema, get)
  }

  private [smithy4s] class PartiallyAppliedOneOf[U, A](private val schema: Schema[A]) extends AnyVal {
    def apply(label: String)(implicit ev: A <:< U, ct: ClassTag[A]): Alt[U, A] = Alt(label, schema, ev, { case a: A => a })
    def apply(label: String, inject: A => U)(project: PartialFunction[U, A]): Alt[U, A] =
      Alt(label, schema, inject, project)
  }

  private [smithy4s] class PartiallyAppliedRefinement[A, B](private val schema: Schema[A]) extends AnyVal {
    def apply[C](c: C)(implicit refinementProvider: RefinementProvider[C, A, B]): Schema[B] = {
      val hint = Hints.Binding.fromValue(c)(refinementProvider.tag)
      RefinementSchema(schema.addHints(hint), refinementProvider.make(c))
    }
  }

  private object OptionDefaultVisitor extends SchemaVisitor.Default[Option] {
    def default[A] : Option[A] = None
    override def option[A](schema: Schema[A]) : Option[Option[A]] = Some(None)
  }
}
