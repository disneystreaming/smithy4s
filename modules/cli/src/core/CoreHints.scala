package smithy4s.cli.core

import smithy4s.ShapeId
import smithy4s.Newtype
import smithy4s.Hints

object CoreHints {

  type FieldName = FieldName.Type

  object FieldName extends Newtype[String] {
    def id: ShapeId = ShapeId("smithy4s.cli", "field_name")

    def require(
      hints: Hints
    ): FieldName = hints.get[FieldName].getOrElse(sys.error("Unknown field name!"))

  }

  type IsNested = IsNested.Type

  object IsNested extends Newtype[Boolean] {
    def id: ShapeId = ShapeId("smithy4s.cli", "is_nested")

    def orFalse(hints: Hints): Boolean = hints.get(IsNested).fold(false)(_.value)
  }

}
