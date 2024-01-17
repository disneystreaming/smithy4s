package smithy4s.example.collision

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.union

sealed trait PackageUnion extends scala.Product with scala.Serializable { self =>
  @inline final def widen: PackageUnion = this
  def $ordinal: Int

  object project {
    def _class: Option[Int] = PackageUnion.ClassCase.alt.project.lift(self).map(_._class)
  }

  def accept[A](visitor: PackageUnion.Visitor[A]): A = this match {
    case value: PackageUnion.ClassCase => visitor._class(value._class)
  }
}
object PackageUnion extends ShapeTag.Companion[PackageUnion] {

  def _class(_class: Int): PackageUnion = ClassCase(_class)

  val id: ShapeId = ShapeId("smithy4s.example.collision", "PackageUnion")

  val hints: Hints = Hints.empty

  final case class ClassCase(_class: Int) extends PackageUnion { final def $ordinal: Int = 0 }

  object ClassCase {
    val hints: Hints = Hints.empty
    val schema: Schema[PackageUnion.ClassCase] = bijection(int.addHints(hints), PackageUnion.ClassCase(_), _._class)
    val alt = schema.oneOf[PackageUnion]("class")
  }

  trait Visitor[A] {
    def _class(value: Int): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def _class(value: Int): A = default
    }
  }

  implicit val schema: Schema[PackageUnion] = union(
    PackageUnion.ClassCase.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
