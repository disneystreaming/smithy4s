package smithy4s.cli.core

import smithy4s.ShapeId
import smithy4s.Newtype
import smithy4s.Hints
import smithy4s.Schema

object CoreHints {

  type FieldName = FieldName.Type

  object FieldName extends Newtype[String] {
    val schema: Schema[Type] = Schema.bijection(Schema.string, apply, _.value)
    def id: ShapeId = ShapeId("smithy4s.cli", "FieldName")

    def require(
                 hints: Hints
               ): FieldName = hints.get[FieldName].getOrElse(sys.error("Unknown field name!"))

  }

  type IsNested = IsNested.Type

  object IsNested extends Newtype[Boolean] {
    val schema: Schema[Type] = Schema.bijection(Schema.boolean, apply, _.value)
    def id: ShapeId = ShapeId("smithy4s.cli", "IsNested")

    def orFalse(hints: Hints): Boolean = hints.get(IsNested).fold(false)(_.value)
  }

}
