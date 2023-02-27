package smithy4s
package http

import smithy4s.PartialData
import smithy.api.HttpPayload
import smithy4s.schema._

/**
 * This construct indicates how a schema is split between http metadata
 * (ie headers, path parameters, query parameters, status code) and body.
 *
 * The schema is split between two different
 */
sealed trait HttpRestSchema[A]

object HttpRestSchema {

  // format: off
  final case class OnlyMetadata[A](schema: Schema[A]) extends HttpRestSchema[A]
  final case class OnlyBody[A](schema: Schema[A]) extends HttpRestSchema[A]
  final case class MetadataAndBody[A](metadataSchema: Schema[PartialData[A]], body: Schema[PartialData[A]]) extends HttpRestSchema[A]
  final case class Empty[A](value: A) extends HttpRestSchema[A]
  // format: on

  def apply[A](fullSchema: Schema[A]): HttpRestSchema[A] = {

    def isMetadataField(field: SchemaField[_, _]): Boolean = HttpBinding
      .fromHints(field.label, field.instance.hints, fullSchema.hints)
      .isDefined

    def isPayloadField(field: SchemaField[_, _]): Boolean =
      field.instance.hints.has[HttpPayload]

    val maybeMetadataSchema: Wedge[Schema[PartialData[A]], Schema[A]] =
      fullSchema.partial(isMetadataField)
    val maybeBodySchema: Wedge[Schema[PartialData[A]], Schema[A]] =
      fullSchema
        .payloadPartial(isPayloadField)
        .orElse(fullSchema.partial(!isMetadataField(_)))

    (maybeMetadataSchema, maybeBodySchema) match {
      case (Wedge.Right(metadataSchema), _) =>
        OnlyMetadata(metadataSchema)
      case (_, Wedge.Right(bodySchema)) =>
        OnlyBody(bodySchema)
      case (Wedge.Left(metadataSchema), Wedge.Left(bodySchema)) =>
        MetadataAndBody(metadataSchema, bodySchema)
      case (_, _) =>
        fullSchema match {
          case Schema.PrimitiveSchema(_, _, Primitive.PUnit) => Empty(())
          case _ => OnlyBody(fullSchema)
        }
    }
  }

}
