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

// todo: caching
private[caliban] object ArgBuilderVisitor
    extends SchemaVisitor.Default[ArgBuilder] {
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
