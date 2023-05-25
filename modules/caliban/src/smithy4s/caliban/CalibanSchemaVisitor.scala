package smithy4s.caliban

import caliban.schema._
import smithy4s.Bijection
import smithy4s.ByteArray
import smithy4s.Document
import smithy4s.Hints
import smithy4s.schema.SchemaVisitor
import smithy4s.schema.Field
import smithy4s.schema.Primitive
import smithy4s.schema.Field.Wrapped
import smithy4s.ShapeId
import smithy4s.Timestamp

// todo: caching
private object CalibanSchemaVisitor
    extends SchemaVisitor.Default[Schema[Any, *]] {
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
