package smithy4s.internals

import smithy4s.kinds.PolyFunction
import smithy4s.schema._
import smithy4s.schema.Schema.StructSchema
import ToPartialSchema._
import smithy4s.Hints

import smithy4s.PartialData

/**
  * When applied on a structure schema, creates a schema that, when compiled into
  * a codec, will only encode/decode a subset of the data, based on the hints
  * of each field.
  *
  * This can be used to only encode some fields of the data into the http body
  *
  * @param payload : whether a single field is being looked for, in which case that
  *                  the first field that matches the criteria is used, and a bijection
  *                  is applied between the schema it holds and the partial data, ensuring
  *                  which allows for the field's schema to be used as "top level" when
  *                  decoding payloads.
  *
  */
case class ToPartialSchema(keep: Hints => Boolean, payload: Boolean)
    extends PolyFunction[Schema, MaybePartialSchema] {

  def apply[S](fa: Schema[S]): MaybePartialSchema[S] = fa match {
    case StructSchema(shapeId, hints, fields, make) =>
      if (payload) {
        fields.zipWithIndex
          .find { case (schemaField, _) =>
            keep(schemaField.instance.hints)
          }
          .flatMap { case (allowedField, index) =>
            allowedField.fold(bijectSingle(index, make))
          }
      } else {
        val allowedFields = fields.zipWithIndex.filter {
          case (schemaField, _) =>
            keep(schemaField.instance.hints)
        }
        val indexes = allowedFields.map(_._2)
        val unsafeAccessFields = allowedFields.map { case (schemaField, _) =>
          schemaField.foldK(fieldFolder[S])
        }
        def const(values: IndexedSeq[Any]): PartialData[S] =
          PartialData.Partial(indexes, values, make)
        Some(StructSchema(shapeId, hints, unsafeAccessFields, const))
      }
    case _ => None
  }

  private def bijectSingle[S](index: Int, make: IndexedSeq[Any] => S) =
    new Field.Folder[Schema, S, Option[Schema[PartialData[S]]]] {
      def onRequired[A](
          label: String,
          instance: Schema[A],
          get: S => A
      ): Option[Schema[PartialData[S]]] = Some {
        Schema.bijection[A, PartialData[S]](
          instance,
          (a: A) => PartialData.Partial(IndexedSeq(index), IndexedSeq(a), make),
          (_: PartialData[S]) match {
            case PartialData.Total(s) => get(s)
            case _                    => codingError
          }
        )
      }
      def onOptional[A](
          label: String,
          instance: Schema[A],
          get: S => Option[A]
      ): Option[Schema[PartialData[S]]] = None
    }

  private def codingError: Nothing =
    sys.error("Coding error: this should not happen on encoding side")

  private def fieldFolder[S] =
    new Field.FolderK[Schema, S, SchemaField[PartialData[S], *]] {
      def onRequired[A](
          label: String,
          instance: Schema[A],
          get: S => A
      ): SchemaField[PartialData[S], A] = {
        def access(product: PartialData[S]): A = product match {
          case PartialData.Total(struct)    => get(struct)
          case PartialData.Partial(_, _, _) => codingError
        }
        instance.required(label, access)
      }
      def onOptional[A](
          label: String,
          instance: Schema[A],
          get: S => Option[A]
      ): SchemaField[PartialData[S], Option[A]] = {
        def access(product: PartialData[S]): Option[A] = product match {
          case PartialData.Total(struct)    => get(struct)
          case PartialData.Partial(_, _, _) => codingError
        }
        instance.optional(label, access)
      }
    }
}

object ToPartialSchema {
  type MaybePartialSchema[A] = Option[Schema[PartialData[A]]]
}
