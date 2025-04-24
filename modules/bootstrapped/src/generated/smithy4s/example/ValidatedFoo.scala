package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class ValidatedFoo(name: ValidatedString = smithy4s.example.ValidatedString.unsafeApply("abc"))

object ValidatedFoo extends ShapeTag.Companion[ValidatedFoo] {
  val id: ShapeId = ShapeId("smithy4s.example", "ValidatedFoo")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(name: ValidatedString): ValidatedFoo = ValidatedFoo(name)

  implicit val schema: Schema[ValidatedFoo] = struct(
    ValidatedString.schema.field[ValidatedFoo]("name", _.name).addHints(smithy.api.Default(smithy4s.Document.fromString("abc"))),
  )(make).withId(id).addHints(hints)
}
