package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.union

sealed trait CheckedOrUnchecked extends scala.Product with scala.Serializable { self =>
  @inline final def widen: CheckedOrUnchecked = this
  def $ordinal: Int

  object project {
    def checked: Option[String] = CheckedOrUnchecked.CheckedCase.alt.project.lift(self).map(_.checked)
    def raw: Option[String] = CheckedOrUnchecked.RawCase.alt.project.lift(self).map(_.raw)
  }

  def accept[A](visitor: CheckedOrUnchecked.Visitor[A]): A = this match {
    case value: CheckedOrUnchecked.CheckedCase => visitor.checked(value.checked)
    case value: CheckedOrUnchecked.RawCase => visitor.raw(value.raw)
  }
}
object CheckedOrUnchecked extends ShapeTag.Companion[CheckedOrUnchecked] {

  def checked(checked: String): CheckedOrUnchecked = CheckedCase(checked)
  def raw(raw: String): CheckedOrUnchecked = RawCase(raw)

  val id: ShapeId = ShapeId("smithy4s.example", "CheckedOrUnchecked")

  val hints: Hints = Hints.empty

  final case class CheckedCase(checked: String) extends CheckedOrUnchecked { final def $ordinal: Int = 0 }
  final case class RawCase(raw: String) extends CheckedOrUnchecked { final def $ordinal: Int = 1 }

  object CheckedCase {
    val hints: Hints = Hints.empty
    val schema: Schema[CheckedOrUnchecked.CheckedCase] = bijection(string.addHints(hints).validated(smithy.api.Pattern(s"^\\w+$$")), CheckedOrUnchecked.CheckedCase(_), _.checked)
    val alt = schema.oneOf[CheckedOrUnchecked]("checked")
  }
  object RawCase {
    val hints: Hints = Hints.empty
    val schema: Schema[CheckedOrUnchecked.RawCase] = bijection(string.addHints(hints), CheckedOrUnchecked.RawCase(_), _.raw)
    val alt = schema.oneOf[CheckedOrUnchecked]("raw")
  }

  trait Visitor[A] {
    def checked(value: String): A
    def raw(value: String): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def checked(value: String): A = default
      def raw(value: String): A = default
    }
  }

  implicit val schema: Schema[CheckedOrUnchecked] = union(
    CheckedOrUnchecked.CheckedCase.alt,
    CheckedOrUnchecked.RawCase.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
