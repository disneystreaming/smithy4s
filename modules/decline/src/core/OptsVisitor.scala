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

package smithy4s.decline.core

import cats.data.{NonEmptyVector, Validated, NonEmptyList}
import cats.implicits._
import com.monovore.decline.Argument
import com.monovore.decline.Opts
import smithy.api.{Documentation, ExternalDocumentation, TimestampFormat}
import smithy4s.{
  Bijection,
  ByteArray,
  Hints,
  Lazy,
  Refinement,
  ShapeId,
  Timestamp
}
import smithy4s.decline.core.CoreHints._
import smithy4s.schema.Alt
import smithy4s.schema.EnumValue
import smithy4s.schema.Primitive
import smithy4s.schema.Primitive._
import smithy4s.schema.Schema._
import smithy4s.schema._

import java.util.UUID
import smithy4s.schema.CollectionTag
import smithy4s.schema.CollectionTag.ListTag

object OptsVisitor extends SchemaVisitor[Opts] { self =>

  private def field[P: Argument](
      hints: Hints
  ): Opts[P] = {
    val fieldName = FieldName.require(hints)
    val isNested = IsNested.orFalse(hints)
    if (isNested)
      Opts.option[P](long = fieldName.value, help = docs(hints))
    else
      Opts.argument[P](fieldName.value)

  }

  private def docs[P: Argument](hints: Hints): String = {
    (hints.get(Documentation).map(_.value).toList ::: hints
      .get(ExternalDocumentation)
      .toList
      .flatMap(_.value.map { case (description, link) =>
        s"$description link: $link"
      }.toList)).mkString("\n")
  }

  private def fieldPlural[P: Argument](
      hints: Hints
  ): Opts[List[P]] = {
    val fieldName = FieldName.require(hints)
    val isNested = IsNested.orFalse(hints)
    if (isNested)
      Opts.options[P](long = fieldName.value, help = docs(hints))
    else
      Opts.arguments[P](fieldName.value)

  }.map(_.toList)

  private def timestampArg(
      fieldName: CoreHints.FieldName,
      formatOpt: Option[TimestampFormat]
  ): Argument[Timestamp] = {
    val format = formatOpt.getOrElse(TimestampFormat.DATE_TIME)
    Argument.from("timestamp") { s =>
      smithy4s.Timestamp
        .parse(s, format)
        .toValidNel(
          s"""Invalid timestamp "$s" for input ${fieldName.value}. Expected format: ${smithy4s.Timestamp
            .showFormat(format)}"""
        )
    }
  }

  private def enumArg[E](
      values: List[EnumValue[E]],
      fieldName: CoreHints.FieldName
  ): Argument[E] =
    Argument.from("enum") { name =>
      values
        .find(_.stringValue == name)
        .map(_.value)
        .toValidNel(
          s"""Unknown value "$name" for input ${fieldName.value}. Allowed values: ${values
            .map(_.stringValue)
            .mkString(", ")}"""
        )
    }

  private def jsonField[A](schema: Schema[A]): Opts[A] = {
    val jsonParser = parseJson(schema)
    implicit val arg: Argument[A] =
      Argument.from("json")(jsonParser(_).toValidatedNel)

    field(schema.hints)
  }

  private def jsonFieldPlural[A](schema: Schema[A]): Opts[List[A]] = {
    val jsonParser = parseJson(schema)
    implicit val arg: Argument[A] =
      Argument.from("json")(jsonParser(_).toValidatedNel)

    fieldPlural(schema.hints)
  }

  private def parseJson[A](schema: Schema[A]): String => Either[String, A] = {
    val capi = new smithy4s.http.json.JsonCodecs()
    val codec = capi.compileCodec(schema)

    s =>
      capi
        .decodeFromByteArray(codec, s.getBytes())
        .leftMap(pe => pe.toString)
  }

  def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): Opts[P] =
    tag match {
      case PByte       => field[Byte](hints)
      case PShort      => field[Short](hints)
      case PFloat      => field[Float](hints)
      case PDouble     => field[Double](hints)
      case PBigInt     => field[BigInt](hints)
      case PBigDecimal => field[BigDecimal](hints)
      case PString     => field[String](hints)
      case PInt        => field[Int](hints)
      case PUUID       => field[UUID](hints)
      case PLong       => field[Long](hints)
      case PUnit       => Opts.unit
      case PTimestamp =>
        implicit val arg: Argument[Timestamp] =
          timestampArg(FieldName.require(hints), hints.get(TimestampFormat))
        field(hints)

      case PBoolean => {
        val fieldName = FieldName.require(hints)

        Opts
          .flag(
            long = fieldName.value,
            help = fieldName.value
          )
          .orFalse
      }

      case PDocument => jsonField(tag.schema(shapeId).addHints(hints))
      case PBlob => {
        implicit val byteArrayArgument = commons.byteArrayArgument
        field[ByteArray](hints)
      }
    }

  private def primitives[P](
      member: Schema.PrimitiveSchema[P]
  ): Opts[List[P]] =
    member.tag match {
      case PByte       => fieldPlural[Byte](member.hints)
      case PShort      => fieldPlural[Short](member.hints)
      case PFloat      => fieldPlural[Float](member.hints)
      case PDouble     => fieldPlural[Double](member.hints)
      case PBigInt     => fieldPlural[BigInt](member.hints)
      case PBigDecimal => fieldPlural[BigDecimal](member.hints)
      case PString     => fieldPlural[String](member.hints)
      case PInt        => fieldPlural[Int](member.hints)
      case PUUID       => fieldPlural[UUID](member.hints)
      case PLong       => fieldPlural[Long](member.hints)
      case PTimestamp =>
        implicit val arg = timestampArg(
          FieldName.require(member.hints),
          member.hints.get(TimestampFormat)
        )

        fieldPlural(member.hints)

      case PBlob =>
        implicit val byteArrayArgument = commons.byteArrayArgument
        fieldPlural[ByteArray](member.hints)

      case PUnit | PBoolean | PDocument => jsonFieldPlural(member)
    }

  def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Schema[A]
  ): Opts[C[A]] =
    tag match {
      case ListTag => list(shapeId, hints, member)
      case CollectionTag.IndexedSeqTag =>
        list(shapeId, hints, member).map(_.toIndexedSeq)
      case CollectionTag.SetTag =>
        list(shapeId, hints, member).map(_.toSet)
      case CollectionTag.VectorTag =>
        list(shapeId, hints, member).map(_.toVector)
    }

  private def list[A](
      shapeId: ShapeId,
      hints: Hints,
      member: Schema[A]
  ): Opts[List[A]] =
    member match {
      case p: Schema.PrimitiveSchema[a] =>
        primitives(p.copy(hints = p.hints ++ hints))
      case b: BijectionSchema[a, b] =>
        list(shapeId, hints, b.underlying).map(_.map(b.bijection.to))
      case s: Schema.RefinementSchema[a, b] =>
        list(shapeId, hints, s.underlying).mapValidated(
          _.traverse(value =>
            Validated.fromEither(s.refinement(value).leftMap(NonEmptyList.one))
          )
        )

      case e: EnumerationSchema[e] =>
        implicit val arg: Argument[e] =
          enumArg(e.values, FieldName.require(hints))

        fieldPlural(hints)

      case _: StructSchema[_] | _: Schema.CollectionSchema[_, _] |
          _: Schema.UnionSchema[_] | _: Schema.LazySchema[_] |
          _: Schema.MapSchema[_, _] =>
        jsonFieldPlural(member.addHints(hints))

    }

  def map[K, V](
      shapeId: ShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ): Opts[Map[K, V]] = jsonField(
    Schema.MapSchema(shapeId, hints, key, value)
  )

  def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): Opts[E] = {
    implicit val arg: Argument[E] = enumArg(values, FieldName.require(hints))

    field(hints)
  }

  def struct[A](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[SchemaField[A, _]],
      make: IndexedSeq[Any] => A
  ): Opts[A] = {
    def structField[X](
        f: smithy4s.schema.Field[Schema, A, X]
    ): Opts[X] = {
      val childHints = Hints(
        FieldName(f.label),
        // Top-level gets a free pass: IsNested(false)
        // Subsequent calls get true
        IsNested(hints.get(IsNested).isDefined)
      )

      f.foldK[Opts](
        new smithy4s.schema.Field.FolderK[Schema, A, Opts] {
          def onRequired[Y](
              label: String,
              instance: Schema[Y],
              get: A => Y
          ): Opts[Y] =
            instance.addHints(childHints).compile[Opts](self)

          def onOptional[Y](
              label: String,
              instance: Schema[Y],
              get: A => Option[Y]
          ): Opts[Option[Y]] = instance
            .addHints(childHints)
            .compile[Opts](self)
            .orNone
        }
      )
    }

    fields
      .traverse(structField(_))
      .map(make)
  }

  def union[A](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[SchemaAlt[A, _]],
      dispatch: Alt.Dispatcher[Schema, A]
  ): Opts[A] = {
    def go[X](
        alt: SchemaAlt[A, X]
    ): Opts[A] = alt.instance
      .addHints(hints)
      .compile[Opts](this)
      .map(alt.inject)

    // todo: probably safe, but make sure
    NonEmptyVector.fromVectorUnsafe(alternatives).reduceMapK { alt =>
      go(alt)
    }
  }

  def lazily[A](suspend: Lazy[Schema[A]]): Opts[A] = jsonField(
    Schema.LazySchema(suspend)
  )

  override def biject[A, B](schema: Schema[A], bijection: Bijection[A, B]) =
    schema.compile[Opts](this).map(bijection.to)

  override def refine[A, B](schema: Schema[A], refinement: Refinement[A, B]) =
    schema
      .compile[Opts](this)
      .mapValidated(a =>
        Validated.fromEither(refinement(a).leftMap(NonEmptyList.one))
      )
}
