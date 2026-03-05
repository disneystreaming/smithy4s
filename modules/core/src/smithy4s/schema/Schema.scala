/*
 *  Copyright 2021-2026 Disney Streaming
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

import scala.reflect.ClassTag

import Schema._

// format: off
sealed trait Schema[A]{
  def shapeId: ShapeId
  def hints: Hints
  final def required[Struct]: PartiallyAppliedRequired[Struct, A] = new PartiallyAppliedRequired[Struct, A](this)
  final def field[Struct]: PartiallyAppliedField[Struct, A] = new PartiallyAppliedField[Struct, A](this)
  final def optional[Struct]: PartiallyAppliedOptional[Struct, A] = new PartiallyAppliedOptional[Struct, A](this)

  final def oneOf[Union]: PartiallyAppliedOneOf[Union, A] = new PartiallyAppliedOneOf[Union,A](this)

  final def compile[F[_]](fk: Schema ~> F): F[A] = fk(this)

  final def addHints(hints: Hint*): Schema[A] = transformHintsLocally(_ ++ Hints(hints:_*))
  final def addMemberHints(hints: Hint*): Schema[A] = transformHintsLocally(_.addMemberHints(Hints(hints:_*)))

  final def addHints(hints: Hints): Schema[A] = transformHintsLocally(_ ++ hints)
  final def addMemberHints(hints: Hints): Schema[A] = transformHintsLocally(_.addMemberHints(hints))

  final def withId(newId: ShapeId): Schema[A] = this match {
    case PrimitiveSchema(_, hints, tag) => PrimitiveSchema(newId, hints, tag)
    case s: CollectionSchema[c, a] => CollectionSchema(newId, s.hints, s.tag, s.member).asInstanceOf[Schema[A]]
    case s: MapSchema[c, k, v] => MapSchema(newId, s.hints, s.tag, s.key, s.value).asInstanceOf[Schema[A]]
    case EnumerationSchema(_, hints, values, tag) => EnumerationSchema(newId, hints, values, tag)
    case StructSchema(_, hints, fields, make) => StructSchema(newId, hints, fields, make)
    case UnionSchema(_, hints, alternatives, dispatch) => UnionSchema(newId, hints, alternatives, dispatch)
    case s: BijectionSchema[_, _] => BijectionSchema(s.underlying.withId(newId), s.bijection)
    case s: RefinementSchema[_, _] => RefinementSchema(s.underlying.withId(newId), s.refinement)
    case LazySchema(suspend) => LazySchema(suspend.map(_.withId(newId)))
    case s: OptionSchema[c, a] => OptionSchema(s.tag, s.underlying.withId(newId)).asInstanceOf[Schema[A]]
  }

  final def withId(namespace: String, name: String): Schema[A] = withId(ShapeId(namespace, name))

  final def transformHintsLocally(f: Hints => Hints): Schema[A] = this match {
    case PrimitiveSchema(shapeId, hints, tag) => PrimitiveSchema(shapeId, f(hints), tag)
    case s: CollectionSchema[c, a] => CollectionSchema(s.shapeId, f(s.hints), s.tag, s.member).asInstanceOf[Schema[A]]
    case s: MapSchema[c, k, v] => MapSchema(s.shapeId, f(s.hints), s.tag, s.key, s.value).asInstanceOf[Schema[A]]
    case EnumerationSchema(shapeId, hints, values, tag) => EnumerationSchema(shapeId, f(hints), values, tag)
    case StructSchema(shapeId, hints, fields, make) => StructSchema(shapeId, f(hints), fields, make)
    case UnionSchema(shapeId, hints, alternatives, dispatch) => UnionSchema(shapeId, f(hints), alternatives, dispatch)
    case s: BijectionSchema[_, _] => BijectionSchema(s.underlying.transformHintsLocally(f), s.bijection)
    case s: RefinementSchema[_, _] => RefinementSchema(s.underlying.transformHintsLocally(f), s.refinement)
    case LazySchema(suspend) => LazySchema(suspend.map(_.transformHintsLocally(f)))
    case s: OptionSchema[c, a] => OptionSchema(s.tag, s.underlying.transformHintsLocally(f)).asInstanceOf[Schema[A]]
  }


  final def transformHintsTransitively(f: Hints => Hints): Schema[A] = transformTransitivelyK(new (Schema ~> Schema) {
    def apply[B](fa: Schema[B]): Schema[B] = {
      val base = fa.transformHintsLocally(f)

      base match {
        case EnumerationSchema(shapeId, hints, tag, values) =>
          EnumerationSchema(shapeId, hints, tag, values.map(_.transformHints(f)))

        case other => other
      }
    }
  })

  def transformTransitivelyK(f: Schema ~> Schema): Schema[A] = compile(new TransitiveCompiler(f))

  final def validated[C](c: C)(implicit constraint: RefinementProvider.Simple[C, A]): Schema[A] = {
    val hint = Hints.Binding.fromValue(c)(constraint.tag)
    RefinementSchema(this.addHints(hint), constraint.make(c))
  }

  final def refined[B]: PartiallyAppliedRefinement[A, B] = new PartiallyAppliedRefinement[A, B](this)

  final def biject[B](bijection: Bijection[A, B]) : Schema[B] = Schema.bijection(this, bijection)
  final def biject[B](to: A => B)(from: B => A) : Schema[B] = Schema.bijection(this, to, from)
  final def option: Schema[Option[A]] = Schema.option(this)

  final def nullable: Schema[Nullable[A]] = Nullable.schema(this)

  final def isOption: Boolean = this match {
    case _: OptionSchema[_, _] => true
    case BijectionSchema(underlying, _) => underlying.isOption
    case RefinementSchema(underlying, _) => underlying.isOption
    case _ => false
  }

  final def getDefault: Option[Document] =
    this.hints.get(smithy.api.Default).map(_.value)

  private final lazy val defaultValue: Option[A] = {
    val maybeDefault = getDefault.flatMap[A] {
      case Document.DNull => this.compile(NullableDefaultVisitor)
      case document => Document.Decoder.fromSchema(this).decode(document).toOption
    }
    maybeDefault.orElse(this.compile(OptionDefaultVisitor))
  }

  final def getDefaultValue: Option[A] = defaultValue

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

  /**
    * Turns this schema into an error schema, using a partial function.
    */
  final def error(unlift: A => Throwable)(lift: PartialFunction[Throwable, A]) : ErrorSchema[A] = ErrorSchema(this, lift.lift, unlift)

  /**
    * Turns this schema into an error schema.
    */
  final def asError(unlift: A => Throwable)(lift: Throwable => Option[A]) : ErrorSchema[A] = ErrorSchema(this, lift, unlift)

}

object Schema {

  def apply[A](implicit ev: Schema[A]): ev.type = ev

  final class PrimitiveSchema[P](val shapeId: ShapeId, val hints: Hints, val tag: Primitive[P]) extends Schema[P] {
    override def equals(obj: Any): Boolean = obj match {
      case that: PrimitiveSchema[_] => this.shapeId == that.shapeId && this.hints == that.hints && this.tag == that.tag
      case _ => false
    }
    override def hashCode(): Int = {
      var result = shapeId.##
      result = 31 * result + hints.##
      result = 31 * result + tag.##
      result
    }
    override def toString: String = s"PrimitiveSchema($shapeId, $hints, $tag)"
  }
  object PrimitiveSchema {
    def apply[P](shapeId: ShapeId, hints: Hints, tag: Primitive[P]): PrimitiveSchema[P] =
      new PrimitiveSchema(shapeId, hints, tag)
    def unapply[P](x: PrimitiveSchema[P]): Some[(ShapeId, Hints, Primitive[P])] =
      Some((x.shapeId, x.hints, x.tag))
  }

  final class CollectionSchema[C[_], A](val shapeId: ShapeId, val hints: Hints, val tag: CollectionTag[C], val member: Schema[A]) extends Schema[C[A]] {
    override def equals(obj: Any): Boolean = obj match {
      case that: CollectionSchema[_, _] => this.shapeId == that.shapeId && this.hints == that.hints && this.tag == that.tag && this.member == that.member
      case _ => false
    }
    override def hashCode(): Int = {
      var result = shapeId.##
      result = 31 * result + hints.##
      result = 31 * result + tag.##
      result = 31 * result + member.##
      result
    }
    override def toString: String = s"CollectionSchema($shapeId, $hints, $tag, $member)"

    def withMember(member: Schema[A]): CollectionSchema[C, A] =
      new CollectionSchema(shapeId, hints, tag, member)
  }
  object CollectionSchema {
    def apply[C[_], A](shapeId: ShapeId, hints: Hints, tag: CollectionTag[C], member: Schema[A]): CollectionSchema[C, A] =
      new CollectionSchema(shapeId, hints, tag, member)
    def unapply[C[_], A](x: CollectionSchema[C, A]): Some[(ShapeId, Hints, CollectionTag[C], Schema[A])] =
      Some((x.shapeId, x.hints, x.tag, x.member))
  }

  final class MapSchema[C[_, _], K, V](val shapeId: ShapeId, val hints: Hints, val tag: MapTag[C], val key: Schema[K], val value: Schema[V]) extends Schema[C[K, V]] {
    override def equals(obj: Any): Boolean = obj match {
      case that: MapSchema[_, _, _] => this.shapeId == that.shapeId && this.hints == that.hints && this.tag == that.tag && this.key == that.key && this.value == that.value
      case _ => false
    }
    override def hashCode(): Int = {
      var result = shapeId.##
      result = 31 * result + hints.##
      result = 31 * result + tag.##
      result = 31 * result + key.##
      result = 31 * result + value.##
      result
    }
    override def toString: String = s"MapSchema($shapeId, $hints, $tag, $key, $value)"

    def withKeyAndValue(key: Schema[K], value: Schema[V]): MapSchema[C, K, V] =
      new MapSchema(shapeId, hints, tag, key, value)
  }
  object MapSchema {
    def apply[C[_, _], K, V](shapeId: ShapeId, hints: Hints, tag: MapTag[C], key: Schema[K], value: Schema[V]): MapSchema[C, K, V] =
      new MapSchema(shapeId, hints, tag, key, value)
    def unapply[C[_, _], K, V](x: MapSchema[C, K, V]): Some[(ShapeId, Hints, MapTag[C], Schema[K], Schema[V])] =
      Some((x.shapeId, x.hints, x.tag, x.key, x.value))
  }

  final class EnumerationSchema[E](val shapeId: ShapeId, val hints: Hints, val tag: EnumTag[E], val values: List[EnumValue[E]]) extends Schema[E] {
    override def equals(obj: Any): Boolean = obj match {
      case that: EnumerationSchema[_] => this.shapeId == that.shapeId && this.hints == that.hints && this.tag == that.tag && this.values == that.values
      case _ => false
    }
    override def hashCode(): Int = {
      var result = shapeId.##
      result = 31 * result + hints.##
      result = 31 * result + tag.##
      result = 31 * result + values.##
      result
    }
    override def toString: String = s"EnumerationSchema($shapeId, $hints, $tag, $values)"
  }
  object EnumerationSchema {
    def apply[E](shapeId: ShapeId, hints: Hints, tag: EnumTag[E], values: List[EnumValue[E]]): EnumerationSchema[E] =
      new EnumerationSchema(shapeId, hints, tag, values)
    def unapply[E](x: EnumerationSchema[E]): Some[(ShapeId, Hints, EnumTag[E], List[EnumValue[E]])] =
      Some((x.shapeId, x.hints, x.tag, x.values))
  }

  final class StructSchema[S](val shapeId: ShapeId, val hints: Hints, val fields: Vector[Field[S, _]], val make: IndexedSeq[Any] => S) extends Schema[S] {
    override def equals(obj: Any): Boolean = obj match {
      case that: StructSchema[_] => this.shapeId == that.shapeId && this.hints == that.hints && this.fields == that.fields && this.make == that.make
      case _ => false
    }
    override def hashCode(): Int = {
      var result = shapeId.##
      result = 31 * result + hints.##
      result = 31 * result + fields.##
      result = 31 * result + make.##
      result
    }
    override def toString: String = s"StructSchema($shapeId, $hints, $fields, $make)"

    def withFields(fields: Vector[Field[S, _]]): StructSchema[S] =
      new StructSchema(shapeId, hints, fields, make)
  }
  object StructSchema {
    def apply[S](shapeId: ShapeId, hints: Hints, fields: Vector[Field[S, _]], make: IndexedSeq[Any] => S): StructSchema[S] =
      new StructSchema(shapeId, hints, fields, make)
    def unapply[S](x: StructSchema[S]): Some[(ShapeId, Hints, Vector[Field[S, _]], IndexedSeq[Any] => S)] =
      Some((x.shapeId, x.hints, x.fields, x.make))
  }

  final class UnionSchema[U](val shapeId: ShapeId, val hints: Hints, val alternatives: Vector[Alt[U, _]], val ordinal: U => Int) extends Schema[U] {
    override def equals(obj: Any): Boolean = obj match {
      case that: UnionSchema[_] => this.shapeId == that.shapeId && this.hints == that.hints && this.alternatives == that.alternatives && this.ordinal == that.ordinal
      case _ => false
    }
    override def hashCode(): Int = {
      var result = shapeId.##
      result = 31 * result + hints.##
      result = 31 * result + alternatives.##
      result = 31 * result + ordinal.##
      result
    }
    override def toString: String = s"UnionSchema($shapeId, $hints, $alternatives, $ordinal)"

    def withAlternatives(alternatives: Vector[Alt[U, _]]): UnionSchema[U] =
      new UnionSchema(shapeId, hints, alternatives, ordinal)
  }
  object UnionSchema {
    def apply[U](shapeId: ShapeId, hints: Hints, alternatives: Vector[Alt[U, _]], ordinal: U => Int): UnionSchema[U] =
      new UnionSchema(shapeId, hints, alternatives, ordinal)
    def unapply[U](x: UnionSchema[U]): Some[(ShapeId, Hints, Vector[Alt[U, _]], U => Int)] =
      Some((x.shapeId, x.hints, x.alternatives, x.ordinal))
  }

  final class OptionSchema[C[_], A](val tag: OptionalTag[C], val underlying: Schema[A]) extends Schema[C[A]] {
    def hints: Hints = underlying.hints
    def shapeId: ShapeId = underlying.shapeId
    override def equals(obj: Any): Boolean = obj match {
      case that: OptionSchema[_, _] => this.tag == that.tag && this.underlying == that.underlying
      case _ => false
    }
    override def hashCode(): Int = {
      var result = tag.##
      result = 31 * result + underlying.##
      result
    }
    override def toString: String = s"OptionSchema($tag, $underlying)"

    def withUnderlying(underlying: Schema[A]): OptionSchema[C, A] =
      new OptionSchema(tag, underlying)
  }
  object OptionSchema {
    def apply[C[_], A](tag: OptionalTag[C], underlying: Schema[A]): OptionSchema[C, A] =
      new OptionSchema(tag, underlying)
    def unapply[C[_], A](x: OptionSchema[C, A]): Some[(OptionalTag[C], Schema[A])] =
      Some((x.tag, x.underlying))
  }

  final class BijectionSchema[A, B](val underlying: Schema[A], val bijection: Bijection[A, B]) extends Schema[B] {
    def shapeId = underlying.shapeId
    def hints = underlying.hints
    override def equals(obj: Any): Boolean = obj match {
      case that: BijectionSchema[_, _] => this.underlying == that.underlying && this.bijection == that.bijection
      case _ => false
    }
    override def hashCode(): Int = {
      var result = underlying.##
      result = 31 * result + bijection.##
      result
    }
    override def toString: String = s"BijectionSchema($underlying, $bijection)"
  }
  object BijectionSchema {
    def apply[A, B](underlying: Schema[A], bijection: Bijection[A, B]): BijectionSchema[A, B] =
      new BijectionSchema(underlying, bijection)
    def unapply[A, B](x: BijectionSchema[A, B]): Some[(Schema[A], Bijection[A, B])] =
      Some((x.underlying, x.bijection))
  }

  final class RefinementSchema[A, B](val underlying: Schema[A], val refinement: Refinement[A, B]) extends Schema[B] {
    def shapeId = underlying.shapeId
    def hints = underlying.hints
    override def equals(obj: Any): Boolean = obj match {
      case that: RefinementSchema[_, _] => this.underlying == that.underlying && this.refinement == that.refinement
      case _ => false
    }
    override def hashCode(): Int = {
      var result = underlying.##
      result = 31 * result + refinement.##
      result
    }
    override def toString: String = s"RefinementSchema($underlying, $refinement)"
  }
  object RefinementSchema {
    def apply[A, B](underlying: Schema[A], refinement: Refinement[A, B]): RefinementSchema[A, B] =
      new RefinementSchema(underlying, refinement)
    def unapply[A, B](x: RefinementSchema[A, B]): Some[(Schema[A], Refinement[A, B])] =
      Some((x.underlying, x.refinement))
  }

  final class LazySchema[A](val suspend: Lazy[Schema[A]]) extends Schema[A] {
    def shapeId: ShapeId = suspend.value.shapeId
    def hints: Hints = suspend.value.hints
    override def equals(obj: Any): Boolean = obj match {
      case that: LazySchema[_] => this.suspend == that.suspend
      case _ => false
    }
    override def hashCode(): Int = suspend.##
    override def toString: String = s"LazySchema($suspend)"
  }
  object LazySchema {
    def apply[A](suspend: Lazy[Schema[A]]): LazySchema[A] =
      new LazySchema(suspend)
    def unapply[A](x: LazySchema[A]): Some[Lazy[Schema[A]]] =
      Some(x.suspend)
  }

  def transformHintsLocallyK(f: Hints => Hints): Schema ~> Schema = new (Schema ~> Schema){
    def apply[A](fa: Schema[A]): Schema[A] = fa.transformHintsLocally(f)
  }

  def transformHintsTransitivelyK(f: Hints => Hints): Schema ~> Schema = new (Schema ~> Schema){
    def apply[A](fa: Schema[A]): Schema[A] = fa.transformHintsTransitively(f)
  }

  /**
   * Transforms this schema, and all the schemas inside it, using the provided function.
   */
  def transformTransitivelyK(f: Schema ~> Schema): Schema ~> Schema = new (Schema ~> Schema) {
    def apply[A](fa: Schema[A]): Schema[A] = fa.transformTransitivelyK(f)
  }

  // format: on
  private final class TransitiveCompiler(
      underlying: Schema ~> Schema
  ) extends (Schema ~> Schema) {

    def apply[A](
        fa: Schema[A]
    ): Schema[A] = fa match {
      case e @ EnumerationSchema(_, _, _, _) => underlying(e)
      case p @ PrimitiveSchema(_, _, _)      => underlying(p)
      case u @ UnionSchema(_, _, _, _) =>
        underlying(u.withAlternatives(u.alternatives.map(handleAlt(_))))
      case s: BijectionSchema[_, _] =>
        underlying(BijectionSchema(this(s.underlying), s.bijection))
      case LazySchema(suspend) =>
        underlying(LazySchema(suspend.map(this.apply)))
      case s: RefinementSchema[_, _] =>
        underlying(RefinementSchema(this(s.underlying), s.refinement))
      case s: CollectionSchema[c, a] =>
        underlying(s.withMember(this(s.member))): Schema[c[a]]
      case m: MapSchema[c, k, v] =>
        underlying(m.withKeyAndValue(key = this(m.key), value = this(m.value))): Schema[c[k, v]]
      case s @ StructSchema(_, _, _, _) =>
        underlying(s.withFields(s.fields.map(handleField(_))))
      case o: OptionSchema[c, a] =>
        underlying(o.withUnderlying(this(o.underlying))): Schema[c[a]]
    }

    private def handleField[S, A](
        field: Field[S, A]
    ): Field[S, A] = field.withSchema(this(field.schema))

    private def handleAlt[S, A](
        alt: Alt[S, A]
    ): Alt[S, A] = alt.withSchema(this(alt.schema))
  }

  // format: off


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
  val timestamp: Schema[time.Timestamp] = Primitive.PTimestamp.schema(prelude, "Timestamp")
  val document: Schema[Document] = Primitive.PDocument.schema(prelude, "Document")
  val uuid: Schema[java.util.UUID] = Primitive.PUUID.schema("alloy", "UUID")
  val localdate: Schema[time.LocalDate] = Primitive.PLocalDate.schema("alloy", "LocalDate")
  val localtime: Schema[time.LocalTime] = Primitive.PLocalTime.schema("alloy", "LocalTime")
  val duration: Schema[scala.concurrent.duration.Duration] = Primitive.PDuration.schema("alloy", "Duration")
  val offsetdatetime: Schema[time.OffsetDateTime] = Primitive.POffsetDateTime.schema("alloy", "OffsetDateTime")

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

  def map[K, V](k: Schema[K], v: Schema[V]): Schema[Map[K, V]] = Schema.MapSchema(placeholder, Hints.empty, MapTag.ScalaMapTag, k, v)
  def seqMap[K, V](k: Schema[K], v: Schema[V]): Schema[MapTag.SeqMapType[K, V]] = Schema.MapSchema(placeholder, Hints.empty, MapTag.SeqMapTag, k, v)
  def sparseMap[K, V](k: Schema[K], v: Schema[V]): Schema[Map[K, Option[V]]] = Schema.MapSchema(placeholder, Hints.empty, MapTag.ScalaMapTag, k, option(v))

  def option[A](s: Schema[A]): Schema[Option[A]] = Schema.OptionSchema(OptionalTag.ScalaOptionTag, s)

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

  def enumeration[E](tag: EnumTag[E], values: List[EnumValue[E]]): Schema[E] =
    Schema.EnumerationSchema(placeholder, Hints.empty, tag, values)

  def stringEnumeration[E <: Enumeration.Value](values: List[E]): Schema[E] =
    enumeration(EnumTag.StringEnum[E](_.stringValue, None), values.map(Enumeration.Value.toSchema(_)))

  def intEnumeration[E <: Enumeration.Value](values: List[E]): Schema[E] =
    enumeration(EnumTag.IntEnum[E](_.intValue, None), values.map(Enumeration.Value.toSchema(_)))

  def openStringEnumeration[E <: Enumeration.Value](values: List[E], unknown: String => E): Schema[E] =
    enumeration(EnumTag.StringEnum[E](_.stringValue, Some(unknown)), values.map(Enumeration.Value.toSchema(_)))

  def openIntEnumeration[E <: Enumeration.Value](values: List[E], unknown: Int => E): Schema[E] =
    enumeration(EnumTag.IntEnum[E](_.intValue, Some(unknown)), values.map(Enumeration.Value.toSchema(_)))

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

  private [smithy4s] class PartiallyAppliedRequired[S, A](private val schema: Schema[A]) extends AnyVal {
    def apply(label: String, get: S => A): Field[S, A] = Field.required(label, schema, get)
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
      apply(refinementProvider.make(c))
    }

    def apply(refinement: Refinement[A, B]): Schema[B] = {
      val hint = Hints.Binding.fromValue(refinement.constraint)(refinement.tag)
      RefinementSchema(schema.addHints(hint), refinement)
    }
  }

  private object OptionDefaultVisitor extends SchemaVisitor.Default[Option] {
    def default[A] : Option[A] = None
    override def option[C[_], A](tag: OptionalTag[C], schema: Schema[A]) : Option[C[A]] = Some(tag.none)
    override def biject[A, B](schema: Schema[A], bijection: Bijection[A, B]): Option[B] = {
      if (schema.hints.has[alloy.Nullable]) None else this.apply(schema).map(bijection.to)
    }
  }

  def operation(id: ShapeId): OperationSchema[Unit, Nothing, Unit, Nothing, Nothing] =
    OperationSchema[Unit, Nothing, Unit, Nothing, Nothing](
      id,
      Hints.empty,
      Schema.unit,
      None,
      Schema.unit,
      None,
      None
    )

  private object NullableDefaultVisitor extends SchemaVisitor.Default[Option] {
    def default[A]: Option[A] = None
    override def biject[A, B](
        schema: Schema[A],
        bijection: Bijection[A, B]
    ): Option[B] = 
      if(schema.hints.has(alloy.Nullable)) {
        schema.compile(this).map(bijection.to)
      } else {
        None
      }
    
    override def option[C[_], A](tag: OptionalTag[C], schema: Schema[A]): Option[C[A]] = Some(tag.none)
  }

}
