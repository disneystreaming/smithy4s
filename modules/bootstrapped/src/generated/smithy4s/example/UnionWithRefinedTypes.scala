package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.union

sealed trait UnionWithRefinedTypes extends scala.Product with scala.Serializable { self =>
  @inline final def widen: UnionWithRefinedTypes = this
  def $ordinal: Int

  object project {
    def age: Option[Age] = UnionWithRefinedTypes.AgeCase.alt.project.lift(self).map(_.age)
    def dogName: Option[smithy4s.refined.Name] = UnionWithRefinedTypes.DogNameCase.alt.project.lift(self).map(_.dogName)
  }

  def accept[A](visitor: UnionWithRefinedTypes.Visitor[A]): A = this match {
    case value: UnionWithRefinedTypes.AgeCase => visitor.age(value.age)
    case value: UnionWithRefinedTypes.DogNameCase => visitor.dogName(value.dogName)
  }
}
object UnionWithRefinedTypes extends ShapeTag.Companion[UnionWithRefinedTypes] {

  def age(age: Age): UnionWithRefinedTypes = AgeCase(age)
  def dogName(dogName: smithy4s.refined.Name): UnionWithRefinedTypes = DogNameCase(dogName)

  val id: ShapeId = ShapeId("smithy4s.example", "UnionWithRefinedTypes")

  val hints: Hints = Hints.empty

  final case class AgeCase(age: Age) extends UnionWithRefinedTypes { final def $ordinal: Int = 0 }
  final case class DogNameCase(dogName: smithy4s.refined.Name) extends UnionWithRefinedTypes { final def $ordinal: Int = 1 }

  object AgeCase {
    val hints: Hints = Hints.empty
    val schema: Schema[UnionWithRefinedTypes.AgeCase] = bijection(Age.schema.addHints(hints), UnionWithRefinedTypes.AgeCase(_), _.age)
    val alt = schema.oneOf[UnionWithRefinedTypes]("age")
  }
  object DogNameCase {
    val hints: Hints = Hints.empty
    val schema: Schema[UnionWithRefinedTypes.DogNameCase] = bijection(DogName.underlyingSchema.addHints(hints), UnionWithRefinedTypes.DogNameCase(_), _.dogName)
    val alt = schema.oneOf[UnionWithRefinedTypes]("dogName")
  }

  trait Visitor[A] {
    def age(value: Age): A
    def dogName(value: smithy4s.refined.Name): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def age(value: Age): A = default
      def dogName(value: smithy4s.refined.Name): A = default
    }
  }

  implicit val schema: Schema[UnionWithRefinedTypes] = union(
    UnionWithRefinedTypes.AgeCase.alt,
    UnionWithRefinedTypes.DogNameCase.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
