package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.bijection
import _root_.smithy4s.schema.Schema.union
import smithy4s.schema.Schema.string

sealed trait CheckedOrUnchecked2 extends _root_.scala.Product with _root_.scala.Serializable { self =>
  @inline final def widen: CheckedOrUnchecked2 = this
  def $ordinal: Int

  object project {
    def checked: Option[String] = CheckedOrUnchecked2.CheckedCase.alt.project.lift(self).map(_.checked)
    def raw: Option[String] = CheckedOrUnchecked2.RawCase.alt.project.lift(self).map(_.raw)
  }

  def accept[A](visitor: CheckedOrUnchecked2.Visitor[A]): A = this match {
    case value: CheckedOrUnchecked2.CheckedCase => visitor.checked(value.checked)
    case value: CheckedOrUnchecked2.RawCase => visitor.raw(value.raw)
  }
}
object CheckedOrUnchecked2 extends ShapeTag.Companion[CheckedOrUnchecked2] {

  def checked(checked: String): CheckedOrUnchecked2 = CheckedCase(checked)
  def raw(raw: String): CheckedOrUnchecked2 = RawCase(raw)

  val id: ShapeId = ShapeId("smithy4s.example", "CheckedOrUnchecked2")

  val hints: Hints = Hints(
    alloy.Untagged(),
  )

  final case class CheckedCase(checked: String) extends CheckedOrUnchecked2 { final def $ordinal: Int = 0 }
  final case class RawCase(raw: String) extends CheckedOrUnchecked2 { final def $ordinal: Int = 1 }

  object CheckedCase {
    val hints: Hints = Hints.empty
    val schema: Schema[CheckedOrUnchecked2.CheckedCase] = bijection(string.addHints(hints).validated(smithy.api.Pattern(s"^\\w+$$")), CheckedOrUnchecked2.CheckedCase(_), _.checked)
    val alt = schema.oneOf[CheckedOrUnchecked2]("checked")
  }
  object RawCase {
    val hints: Hints = Hints.empty
    val schema: Schema[CheckedOrUnchecked2.RawCase] = bijection(string.addHints(hints), CheckedOrUnchecked2.RawCase(_), _.raw)
    val alt = schema.oneOf[CheckedOrUnchecked2]("raw")
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

  implicit val schema: Schema[CheckedOrUnchecked2] = union(
    CheckedOrUnchecked2.CheckedCase.alt,
    CheckedOrUnchecked2.RawCase.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
