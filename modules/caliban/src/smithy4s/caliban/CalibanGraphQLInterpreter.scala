package smithy4s.caliban

import caliban.Value.NullValue
import caliban._
import caliban.interop.cats.implicits._
import caliban.schema._
import cats.effect.kernel.Async
import cats.effect.std.Dispatcher
import cats.implicits._
import smithy4s.Bijection
import smithy4s.ByteArray
import smithy4s.Document
import smithy4s.Hints
import smithy4s.Service
import smithy4s.schema.SchemaVisitor
import smithy4s.schema.Field
import smithy4s.schema.Primitive
import smithy4s.schema.Field.Wrapped
import smithy4s.ShapeId
import smithy4s.Timestamp

object CalibanGraphQLInterpreter {
  def server[Alg[_[_, _, _, _, _]], F[_]: Async: Dispatcher](implicit
      service: Service[Alg]
  ): Schema[Any, service.Impl[F]] =
    // todo: renaming to account for graphql conventions (camelCase ops etc.)?
    Schema.obj(name = service.id.name, description = None)(implicit fa =>
      service.endpoints.map { endpointToSchema[F].apply(service)(_) }
    )

  private def fToSchema[F[_]: Async: Dispatcher, O](
      schema: smithy4s.Schema[O]
  ): Schema[Any, F[O]] = {
    implicit val underlying: Schema[Any, O] = schema.compile(GVisitor)
    implicitly
  }

  private def endpointToSchema[
      F[_]: Async: Dispatcher
  ] = new EndpointToSchemaPartiallyApplied[F]

  // "partially-applied type" pattern used here to give the compiler a hint about what F is
  // but let it infer the remaining type parameters
  final class EndpointToSchemaPartiallyApplied[
      F[_]: Async: Dispatcher
  ] private[caliban] {
    def apply[
        Alg[_[_, _, _, _, _]],
        I,
        E,
        O,
        SI,
        SO
    ](service: Service[Alg])(
        e: service.Endpoint[I, E, O, SI, SO]
    )(implicit fa: FieldAttributes) = {

      Schema.fieldWithArgs[service.Impl[F], I](e.name) { alg =>
        val interp = service.toPolyFunction(alg)

        i => interp(e.wrap(i))
      }(
        Schema.functionSchema[Any, Any, I, F[O]](
          e.input.compile(ArgBuilderVisitor),
          e.input.compile(GVisitor),
          fToSchema(e.output)
        ),
        fa
      )
    }
  }
}

// todo: caching
private object GVisitor extends SchemaVisitor.Default[Schema[Any, *]] {
  // todo: remaining cases
  override def default[A]: Schema[Any, A] = sys.error("unsupported schema")

  override def biject[A, B](
      schema: smithy4s.Schema[A],
      bijection: Bijection[A, B]
  ): Schema[Any, B] = schema.compile(this).contramap(bijection.from)

  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): Schema[Any, P] = {
    implicit val byteSchema: Schema[Any, Byte] = null // TODO
    implicit val blobSchema: Schema[Any, ByteArray] = null // TODO
    implicit val documentSchema: Schema[Any, Document] = null // TODO
    implicit val timestampSchema: Schema[Any, Timestamp] = null // TODO

    Primitive.deriving[Schema[Any, *]].apply(tag)
  }

  private def field[S, A](
      f: Field[Schema[Any, *], S, A]
  )(implicit fa: FieldAttributes) = {
    val schema = f
      .instanceA(new Field.ToOptional[Schema[Any, *]] {

        override def apply[A0](
            fa: Schema[Any, A0]
        ): Wrapped[Schema[Any, *], Option, A0] = Schema.optionSchema(fa)

      })

    Schema.field(f.label)(f.get)(
      schema,
      fa
    )
  }
  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[Field[smithy4s.Schema, S, ?]],
      make: IndexedSeq[Any] => S
  ): Schema[Any, S] =
    Schema.obj(shapeId.name, None) { implicit fa =>
      fields
        .map(_.mapK(this))
        .map(field(_))
        .toList
    }

}

// todo: caching
object ArgBuilderVisitor extends SchemaVisitor.Default[ArgBuilder] {
  // todo: remaining cases
  override def default[A]: ArgBuilder[A] = sys.error("unsupported schema")

  override def biject[A, B](
      schema: smithy4s.Schema[A],
      bijection: Bijection[A, B]
  ): ArgBuilder[B] = schema.compile(this).map(bijection.to)

  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[Field[smithy4s.Schema, S, ?]],
      make: IndexedSeq[Any] => S
  ): ArgBuilder[S] = {
    val fieldsCompiled = fields.map { f =>
      f.label ->
        f.mapK(this)
          .instanceA(
            new Field.ToOptional[ArgBuilder] {
              override def apply[A0](
                  fa: ArgBuilder[A0]
              ): Wrapped[ArgBuilder, Option, A0] = ArgBuilder.option(fa)

            }
          )
    }

    { case InputValue.ObjectValue(objectFields) =>
      fieldsCompiled
        .traverse { case (label, f) =>
          f.build(objectFields.getOrElse(label, NullValue))
        }
        .map(make)
    // todo other cases
    }
  }

  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): ArgBuilder[P] = {
    implicit val shortArgBuilder: ArgBuilder[Short] = null // todo
    implicit val byteArgBuilder: ArgBuilder[Byte] = null // todo
    implicit val byteArrayArgBuilder: ArgBuilder[ByteArray] = null // todo
    implicit val documentArgBuilder: ArgBuilder[Document] = null // todo
    implicit val timestampArgBuilder: ArgBuilder[Timestamp] =
      null // todo

    Primitive.deriving[ArgBuilder].apply(tag)
  }
}
