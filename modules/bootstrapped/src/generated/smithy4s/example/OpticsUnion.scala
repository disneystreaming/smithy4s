package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Prism
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.union

sealed trait OpticsUnion extends scala.Product with scala.Serializable { self =>
  @inline final def widen: OpticsUnion = this
  def $ordinal: Int

  object project {
    def one: Option[OpticsStructure] = OpticsUnion.OneCase.alt.project.lift(self).map(_.one)
  }

  def accept[A](visitor: OpticsUnion.Visitor[A]): A = this match {
    case value: OpticsUnion.OneCase => visitor.one(value.one)
  }
}
object OpticsUnion extends ShapeTag.Companion[OpticsUnion] {

  def one(one: OpticsStructure): OpticsUnion = OneCase(one)

  val id: ShapeId = ShapeId("smithy4s.example", "OpticsUnion")

  val hints: Hints = Hints.empty

  object optics {
    val one: Prism[OpticsUnion, OpticsStructure] = Prism.partial[OpticsUnion, OpticsStructure]{ case OpticsUnion.OneCase(t) => t }(OpticsUnion.OneCase.apply)
  }

  final case class OneCase(one: OpticsStructure) extends OpticsUnion { final def $ordinal: Int = 0 }

  object OneCase {
    val hints: Hints = Hints.empty
    val schema: Schema[OpticsUnion.OneCase] = bijection(OpticsStructure.schema.addHints(hints), OpticsUnion.OneCase(_), _.one)
    val alt = schema.oneOf[OpticsUnion]("one")
  }

  trait Visitor[A] {
    def one(value: OpticsStructure): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def one(value: OpticsStructure): A = default
    }
  }

  implicit val schema: Schema[OpticsUnion] = union(
    OpticsUnion.OneCase.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
