package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string
import smithy4s.NewtypeValidated
import smithy4s.RefinementProvider

object CityId extends NewtypeValidated[String] {

  private val refinement = RefinementProvider.make[String](smithy.api.Pattern(s"^[A-Za-z0-9 ]+$$"))

  def apply(a: String): Either[String,Type] = refinement.apply(a).map(unsafeApply)

  val id: ShapeId = ShapeId("smithy4s.example", "CityId")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[String] = Schema.RefinementSchema(string.withId(id).addHints(hints), refinement)
  implicit val schema: Schema[CityId] = bijection(underlyingSchema, asBijectionUnsafe)
}
