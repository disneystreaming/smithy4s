package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.union

sealed trait PersonUnion extends scala.Product with scala.Serializable { self =>
  @inline final def widen: PersonUnion = this
  def $ordinal: Int

  object project {
    def p: Option[PersonUnion.OtherPerson] = PersonUnion.OtherPerson.alt.project.lift(self)
  }

  def accept[A](visitor: PersonUnion.Visitor[A]): A = this match {
    case value: PersonUnion.OtherPerson => visitor.p(value)
  }
}
object PersonUnion extends ShapeTag.Companion[PersonUnion] {

  def otherPerson(name: String):OtherPerson = OtherPerson(name)

  val id: ShapeId = ShapeId("smithy4s.example", "PersonUnion")

  val hints: Hints = Hints.empty

  final case class OtherPerson(name: String) extends PersonUnion {
    def $ordinal: Int = 0
  }

  object OtherPerson {
    val id: ShapeId = ShapeId("smithy4s.example", "OtherPerson")

    val hints: Hints = Hints.empty

    // constructor using the original order from the spec
    private def make(name: String): OtherPerson = OtherPerson(name)

    val schema: Schema[OtherPerson] = struct(
      string.required[OtherPerson]("name", _.name),
    )(make).withId(id).addHints(hints)

    val alt = schema.oneOf[PersonUnion]("p")
  }


  trait Visitor[A] {
    def p(value: PersonUnion.OtherPerson): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def p(value: PersonUnion.OtherPerson): A = default
    }
  }

  implicit val schema: Schema[PersonUnion] = union(
    PersonUnion.OtherPerson.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
