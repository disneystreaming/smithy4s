package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Prism
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.union

sealed trait CheckedOrUnchecked extends scala.Product with scala.Serializable {
  @inline final def widen: CheckedOrUnchecked = this
}
object CheckedOrUnchecked extends ShapeTag.Companion[CheckedOrUnchecked] {
  val id: ShapeId = ShapeId("smithy4s.example", "CheckedOrUnchecked")

  val hints: Hints = Hints.empty

  object Prisms {
    val checked = Prism.partial[CheckedOrUnchecked, String]{ case CheckedCase(t) => t }(CheckedCase.apply)
    val raw = Prism.partial[CheckedOrUnchecked, String]{ case RawCase(t) => t }(RawCase.apply)
  }

  final case class CheckedCase(checked: String) extends CheckedOrUnchecked
  final case class RawCase(raw: String) extends CheckedOrUnchecked

  object CheckedCase {
    val hints: Hints = Hints.empty
    val schema: Schema[CheckedCase] = bijection(string.addHints(hints).validated(smithy.api.Pattern("^\\w+$")), CheckedCase(_), _.checked)
    val alt = schema.oneOf[CheckedOrUnchecked]("checked")
  }
  object RawCase {
    val hints: Hints = Hints.empty
    val schema: Schema[RawCase] = bijection(string.addHints(hints), RawCase(_), _.raw)
    val alt = schema.oneOf[CheckedOrUnchecked]("raw")
  }

  implicit val schema: Schema[CheckedOrUnchecked] = union(
    CheckedCase.alt,
    RawCase.alt,
  ){
    case c: CheckedCase => CheckedCase.alt(c)
    case c: RawCase => RawCase.alt(c)
  }.withId(id).addHints(hints)
}
