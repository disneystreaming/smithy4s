package smithy4s.cli.core

import cats.MonadThrow
import cats.data.NonEmptyVector
import cats.implicits._
import com.monovore.decline.Argument
import com.monovore.decline.Opts
import smithy.api.Documentation
import smithy.api.TimestampFormat
import smithy4s.Hints
import smithy4s.Lazy
import smithy4s.Refinement
import smithy4s.ShapeId
import smithy4s.Timestamp
import smithy4s.cli.core.CoreHints._
import smithy4s.schema.Alt
import smithy4s.schema.EnumValue
import smithy4s.schema.Primitive
import smithy4s.schema.Primitive.PBigDecimal
import smithy4s.schema.Primitive.PBigInt
import smithy4s.schema.Primitive.PBlob
import smithy4s.schema.Primitive.PBoolean
import smithy4s.schema.Primitive.PByte
import smithy4s.schema.Primitive.PDocument
import smithy4s.schema.Primitive.PDouble
import smithy4s.schema.Primitive.PFloat
import smithy4s.schema.Primitive.PInt
import smithy4s.schema.Primitive.PLong
import smithy4s.schema.Primitive.PShort
import smithy4s.schema.Primitive.PString
import smithy4s.schema.Primitive.PTimestamp
import smithy4s.schema.Primitive.PUUID
import smithy4s.schema.Primitive.PUnit
import smithy4s.schema.Schema
import smithy4s.schema.Schema.BijectionSchema
import smithy4s.schema.Schema.EnumerationSchema
import smithy4s.schema.Schema.StructSchema
import smithy4s.schema.SchemaAlt
import smithy4s.schema.SchemaField
import smithy4s.schema.SchemaVisitor
import java.util.UUID

object OptsVisitor {
  type OptsF[F[_], A] = Opts[F[A]]
}

import OptsVisitor.OptsF

class OptsVisitor[F[_]: MonadThrow: PathOps] extends SchemaVisitor[OptsF[F, *]] { self =>

  private def field[P: Argument](
    hints: Hints
  ): Opts[F[P]] = {

    val fieldName = FieldName.require(hints)
    val isNested = IsNested.orFalse(hints)
    val doc = hints.get(Documentation).fold("")(_.value)

    {
      if (isNested)
        Opts.option[P](long = fieldName.value, help = doc)
      else
        Opts.argument[P](fieldName.value)
    }
  }.map(_.pure[F])

  private def fieldPlural[P: Argument](
    hints: Hints
  ): Opts[F[List[P]]] = {
    val fieldName = FieldName.require(hints)
    val isNested = IsNested.orFalse(hints)
    val doc = hints.get(Documentation).fold("")(_.value)

    {
      if (isNested)
        Opts.options[P](long = fieldName.value, help = doc)
      else
        Opts.arguments[P](fieldName.value)
    }
  }.map(_.toList.pure[F])

  private def timestampArg(
    fieldName: CoreHints.FieldName,
    formatOpt: Option[TimestampFormat],
  ): Argument[Timestamp] = {
    val format = formatOpt.getOrElse(TimestampFormat.DATE_TIME)
    Argument.from("timestamp") { s =>
      smithy4s
        .Timestamp
        .parse(s, format)
        .toValidNel(
          s"""Invalid timestamp "$s" for input ${fieldName.value}. Expected format: ${smithy4s
            .Timestamp
            .showFormat(format)}"""
        )
    }
  }

  private def enumArg[E](
    values: List[EnumValue[E]],
    fieldName: CoreHints.FieldName,
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

  private def jsonField[A](schema: Schema[A]): Opts[F[A]] = {
    val jsonParser = parseJson(schema)
    implicit val arg: Argument[A] = Argument.from("json")(jsonParser(_).toValidatedNel)

    field(schema.hints)
  }

  private def jsonFieldPlural[A](schema: Schema[A]): Opts[F[List[A]]] = {
    val jsonParser = parseJson(schema)
    implicit val arg: Argument[A] = Argument.from("json")(jsonParser(_).toValidatedNel)

    fieldPlural(schema.hints)
  }

  private def parseJson[A](schema: Schema[A]): String => Either[String, A] = {
    val capi = smithy4s.http.json.codecs()
    val codec = capi.compileCodec(schema)

    s =>
      capi
        .decodeFromByteArray(codec, s.getBytes())
        .leftMap(pe => pe.toString)
  }

  def primitive[P](shapeId: ShapeId, hints: Hints, tag: Primitive[P]): Opts[F[P]] =
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
      case PUnit       => Opts.unit.map(_.pure[F])
      case PTimestamp =>
        implicit val arg = timestampArg(FieldName.require(hints), hints.get(TimestampFormat))
        field(hints)

      case PBoolean =>
        val fieldName: FieldName = FieldName.require(hints)
        Opts
          .flag(
            long = fieldName.value,
            help = fieldName.value,
          )
          .orFalse
          .map(_.pure[F])

      case PDocument => jsonField(tag.schema(shapeId).addHints(hints))
      case PBlob     => field[String](hints).map(_.flatMap(PathOps[F].path(_)))
    }

  private def primitives[P](
    member: Schema.PrimitiveSchema[P]
  ): Opts[F[List[P]]] =
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
          member.hints.get(TimestampFormat),
        )

        fieldPlural(member.hints)

      case PBlob => fieldPlural[String](member.hints).map(_.flatMap(_.traverse(PathOps[F].path(_))))

      case PUnit | PBoolean | PDocument => jsonFieldPlural(member)
    }

  def list[A](shapeId: ShapeId, hints: Hints, member: Schema[A]): Opts[F[List[A]]] =
    member match {
      case p: Schema.PrimitiveSchema[a] => primitives(p.copy(hints = p.hints ++ hints))
      case b: BijectionSchema[a, b] => list(shapeId, hints, b.underlying).map(_.map(_.map(b.to)))
      case s: Schema.SurjectionSchema[a, b] =>
        list(shapeId, hints, s.underlying).map(
          _.flatMap(_.traverse(s.refinement(_).leftMap(RefinementFailed(_)).liftTo[F]))
        )

      case e: EnumerationSchema[e] =>
        implicit val arg: Argument[e] = enumArg(e.values, FieldName.require(hints))

        fieldPlural(hints)

      case _: StructSchema[_] | _: Schema.ListSchema[_] | _: Schema.SetSchema[_] |
          _: Schema.UnionSchema[_] | _: Schema.LazySchema[_] | _: Schema.MapSchema[_, _] =>
        jsonFieldPlural(member.addHints(hints))

    }

  def set[A](
    shapeId: ShapeId,
    hints: Hints,
    member: Schema[A],
  ): Opts[F[Set[A]]] = list(shapeId, hints, member).map(_.map(_.toSet))

  def map[K, V](
    shapeId: ShapeId,
    hints: Hints,
    key: Schema[K],
    value: Schema[V],
  ): Opts[F[Map[K, V]]] = jsonField(Schema.MapSchema(shapeId, hints, key, value))

  def enumeration[E](
    shapeId: ShapeId,
    hints: Hints,
    values: List[EnumValue[E]],
    total: E => EnumValue[E],
  ): Opts[F[E]] = {
    implicit val arg: Argument[E] = enumArg(values, FieldName.require(hints))

    field(hints)
  }

  def struct[A](
    shapeId: ShapeId,
    hints: Hints,
    fields: Vector[SchemaField[A, _]],
    make: IndexedSeq[Any] => A,
  ): Opts[F[A]] = {
    def structField[X](
      f: smithy4s.schema.Field[Schema, A, X]
    ): Opts[F[X]] = {
      val childHints = Hints(
        FieldName(f.label),
        // Top-level gets a free pass: IsNested(false)
        // Subsequent calls get true
        IsNested(hints.get(IsNested).isDefined),
      )

      f.foldK[OptsF[F, *]](new smithy4s.schema.Field.FolderK[Schema, A, OptsF[F, *]] {
        def onRequired[Y](
          label: String,
          instance: Schema[Y],
          get: A => Y,
        ): Opts[F[Y]] = instance.addHints(childHints).compile[OptsF[F, *]](self)

        def onOptional[Y](
          label: String,
          instance: Schema[Y],
          get: A => Option[Y],
        ): Opts[F[Option[Y]]] = instance
          .addHints(childHints)
          .compile[OptsF[F, *]](self)
          .orNone
          .map(_.sequence)
      })
    }

    fields
      .traverse(structField(_).map(_.widen[Any]))
      .map(_.sequence.map(make))
  }

  def union[A](
    shapeId: ShapeId,
    hints: Hints,
    alternatives: Vector[SchemaAlt[A, _]],
    dispatch: A => Alt.SchemaAndValue[A, _],
  ): Opts[F[A]] = {
    def go[X](alt: Alt[Schema, A, X]): Opts[F[A]] = alt.instance
      .addHints(hints)
      .compile[OptsF[F, *]](this)
      .map(_.map(alt.inject))

    // todo: probably safe, but make sure
    NonEmptyVector.fromVectorUnsafe(alternatives).reduceMapK { alt =>
      go(alt)
    }
  }

  def biject[A, B](
    schema: Schema[A],
    to: A => B,
    from: B => A,
  ): Opts[F[B]] = schema.compile[OptsF[F, *]](this).map(_.map(to))

  def surject[A, B](
    schema: Schema[A],
    to: Refinement[A, B],
    from: B => A,
  ): Opts[F[B]] = schema
    .compile[OptsF[F, *]](this)
    .map(_.flatMap(to(_).leftMap(RefinementFailed(_)).liftTo[F]))

  def lazily[A](suspend: Lazy[Schema[A]]): Opts[F[A]] = jsonField(Schema.LazySchema(suspend))

}
