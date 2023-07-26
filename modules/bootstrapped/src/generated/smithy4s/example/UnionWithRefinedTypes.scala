package smithy4s.example

import smithy4s.Bijection
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.union

sealed trait UnionWithRefinedTypes extends scala.Product with scala.Serializable {
  @inline final def widen: UnionWithRefinedTypes = this
  def _ordinal: Int
}
object UnionWithRefinedTypes extends ShapeTag.Companion[UnionWithRefinedTypes] {
  final case class AgeCase(age: Age) extends UnionWithRefinedTypes { final def _ordinal: Int = 0 }
  final case class DogNameCase(dogName: smithy4s.refined.Name) extends UnionWithRefinedTypes { final def _ordinal: Int = 1 }

  object AgeCase {
    implicit val fromValue: Bijection[Age, AgeCase] = Bijection(AgeCase(_), _.age)
    implicit val toValue: Bijection[AgeCase, Age] = fromValue.swap
    val schema: Schema[AgeCase] = bijection(Age.schema, fromValue)
  }
  object DogNameCase {
    implicit val fromValue: Bijection[smithy4s.refined.Name, DogNameCase] = Bijection(DogNameCase(_), _.dogName)
    implicit val toValue: Bijection[DogNameCase, smithy4s.refined.Name] = fromValue.swap
    val schema: Schema[DogNameCase] = bijection(DogName.underlyingSchema, fromValue)
  }

  val age = AgeCase.schema.oneOf[UnionWithRefinedTypes]("age")
  val dogName = DogNameCase.schema.oneOf[UnionWithRefinedTypes]("dogName")

  implicit val schema: Schema[UnionWithRefinedTypes] = union(
    age,
    dogName,
  ){
    _._ordinal
  }
  .withId(ShapeId("smithy4s.example", "UnionWithRefinedTypes"))
}
