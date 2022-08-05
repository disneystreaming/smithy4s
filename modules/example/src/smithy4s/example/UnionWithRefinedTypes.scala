package smithy4s.example

import smithy4s.schema.Schema._

sealed trait UnionWithRefinedTypes extends scala.Product with scala.Serializable {
  @inline final def widen: UnionWithRefinedTypes = this
}
object UnionWithRefinedTypes extends smithy4s.ShapeTag.Companion[UnionWithRefinedTypes] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "UnionWithRefinedTypes")

  val hints : smithy4s.Hints = smithy4s.Hints.empty

  case class AgeCase(age: Age) extends UnionWithRefinedTypes
  case class DogNameCase(dogName: smithy4s.example.refined.Name) extends UnionWithRefinedTypes

  object AgeCase {
    val hints : smithy4s.Hints = smithy4s.Hints.empty
    val schema: smithy4s.Schema[AgeCase] = bijection(Age.schema.addHints(hints), AgeCase(_), _.age)
    val alt = schema.oneOf[UnionWithRefinedTypes]("age")
  }
  object DogNameCase {
    val hints : smithy4s.Hints = smithy4s.Hints.empty
    val schema: smithy4s.Schema[DogNameCase] = bijection(DogName.underlyingSchema.addHints(hints), DogNameCase(_), _.dogName)
    val alt = schema.oneOf[UnionWithRefinedTypes]("dogName")
  }

  implicit val schema: smithy4s.Schema[UnionWithRefinedTypes] = union(
    AgeCase.alt,
    DogNameCase.alt,
  ){
    case c : AgeCase => AgeCase.alt(c)
    case c : DogNameCase => DogNameCase.alt(c)
  }.withId(id).addHints(hints)
}