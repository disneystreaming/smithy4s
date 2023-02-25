package smithy4s.internals

import smithy4s.kinds.PolyFunction
import smithy4s.schema._
import smithy4s.schema.Schema._
import ToPartialSchema._

import smithy4s.PartialData
import smithy4s.Wedge

/**
  * When applied on a structure schema, creates a schema that, when compiled into
  * a codec, will only encode/decode a subset of the data, based on the hints
  * of each field.
  *
  * This can be used to only encode some fields of the data into the http body
  *
  * Returns a Wedge indicating whether :
  *   * no field match the condition
  *   * some fields match the condition
  *   * all fields match the condition
  *
  * @param payload : whether a single field is being looked for, in which case that
  *                  the first field that matches the criteria is used, and a bijection
  *                  is applied between the schema it holds and the partial data, ensuring
  *                  which allows for the field's schema to be used as "top level" when
  *                  decoding payloads.
  *
  */
case class ToPartialSchema(keep: SchemaField[_, _] => Boolean, payload: Boolean)
    extends PolyFunction[Schema, MaybePartialSchema] {

  def apply[S](fa: Schema[S]): MaybePartialSchema[S] = {
    fa match {
      case StructSchema(shapeId, hints, fields, make) =>
        if (payload) {
          fields.zipWithIndex
            .find { case (schemaField, _) =>
              keep(schemaField)
            }
            .map { case (allowedField, index) =>
              allowedField.fold(
                bijectSingle(index, make, total = fields.size == 1)
              )
            }
            .getOrElse {
              Wedge.Empty
            }
        } else {
          val allowedFields = fields.zipWithIndex.filter {
            case (schemaField, _) => keep(schemaField)
          }
          if (allowedFields.size == fields.size) {
            Wedge.Right(fa)
          } else {
            val indexes = allowedFields.map(_._2)
            val unsafeAccessFields = allowedFields.map {
              case (schemaField, _) =>
                schemaField.foldK(fieldFolder[S])
            }
            def const(values: IndexedSeq[Any]): PartialData[S] =
              PartialData.Partial(indexes, values, make)
            Wedge.Left(StructSchema(shapeId, hints, unsafeAccessFields, const))
          }
        }
      case BijectionSchema(underlying, bijection) =>
        apply(underlying).bimap(
          _.biject(_.map(bijection.to), _.map(bijection.from)),
          _.biject(bijection)
        )
      case _ => Wedge.Empty
    }
  }

  private def bijectSingle[S](
      index: Int,
      make: IndexedSeq[Any] => S,
      total: Boolean
  ) =
    new Field.Folder[Schema, S, MaybePartialSchema[S]] {
      def onRequired[A](
          label: String,
          instance: Schema[A],
          get: S => A
      ): MaybePartialSchema[S] = if (total) {
        Wedge.Right { instance.biject(a => make(IndexedSeq(a)), get) }
      } else
        Wedge.Left {
          Schema.bijection[A, PartialData[S]](
            instance,
            (a: A) =>
              PartialData.Partial(IndexedSeq(index), IndexedSeq(a), make),
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
      ): MaybePartialSchema[S] = Wedge.Empty
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
  type MaybePartialSchema[A] = Wedge[Schema[PartialData[A]], Schema[A]]
}
