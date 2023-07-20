package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.union

sealed trait CheckedOrUnchecked2 extends scala.Product with scala.Serializable {
  @inline final def widen: CheckedOrUnchecked2 = this
}
object CheckedOrUnchecked2 extends ShapeTag.Companion[CheckedOrUnchecked2] {
  val id: ShapeId = ShapeId("smithy4s.example", "CheckedOrUnchecked2")

  val hints: Hints = Hints(
    alloy.Untagged(),
  )

  final case class CheckedCase(checked: String) extends CheckedOrUnchecked2
  final case class RawCase(raw: String) extends CheckedOrUnchecked2

  object CheckedCase {
    val hints: Hints = Hints.empty
    val schema: Schema[CheckedCase] = bijection(string.addHints(hints).validated(smithy.api.Pattern("^\\w+$")), CheckedCase(_), _.checked)
    val alt = schema.oneOf[CheckedOrUnchecked2]("checked")
  }
  object RawCase {
    val hints: Hints = Hints.empty
    val schema: Schema[RawCase] = bijection(string.addHints(hints), RawCase(_), _.raw)
    val alt = schema.oneOf[CheckedOrUnchecked2]("raw")
  }

  implicit val schema: Schema[CheckedOrUnchecked2] = union(
    CheckedCase.alt,
    RawCase.alt,
  ){
    case c: CheckedCase => CheckedCase.alt(c)
    case c: RawCase => RawCase.alt(c)
  }.withId(id).addHints(hints)
}
