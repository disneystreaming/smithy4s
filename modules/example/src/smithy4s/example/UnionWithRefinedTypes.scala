package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.example.refined.Name
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.union

sealed trait UnionWithRefinedTypes extends scala.Product with scala.Serializable {
  @inline final def widen: UnionWithRefinedTypes = this
}
object UnionWithRefinedTypes extends ShapeTag.Companion[UnionWithRefinedTypes] {
  val id: ShapeId = ShapeId("smithy4s.example", "UnionWithRefinedTypes")

  val hints : Hints = Hints.empty

  case class AgeCase(age: Age) extends UnionWithRefinedTypes
  case class DogNameCase(dogName: Name) extends UnionWithRefinedTypes

  object AgeCase {
    val hints : Hints = Hints.empty
    val schema: Schema[AgeCase] = bijection(Age.schema.addHints(hints), AgeCase(_), _.age)
    val alt = schema.oneOf[UnionWithRefinedTypes]("age")
  }
  object DogNameCase {
    val hints : Hints = Hints.empty
    val schema: Schema[DogNameCase] = bijection(DogName.underlyingSchema.addHints(hints), DogNameCase(_), _.dogName)
    val alt = schema.oneOf[UnionWithRefinedTypes]("dogName")
  }

  implicit val schema: Schema[UnionWithRefinedTypes] = union(
    AgeCase.alt,
    DogNameCase.alt,
  ){
    case c : AgeCase => AgeCase.alt(c)
    case c : DogNameCase => DogNameCase.alt(c)
  }.withId(id).addHints(hints)
}