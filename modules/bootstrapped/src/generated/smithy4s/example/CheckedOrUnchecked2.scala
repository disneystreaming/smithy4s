package smithy4s.example

import alloy.Untagged
import smithy.api.Pattern
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.union

sealed trait CheckedOrUnchecked2 extends scala.Product with scala.Serializable {
  @inline final def widen: CheckedOrUnchecked2 = this
  def _ordinal: Int
}
object CheckedOrUnchecked2 extends ShapeTag.Companion[CheckedOrUnchecked2] {
  final case class CheckedCase(checked: String) extends CheckedOrUnchecked2 { final def _ordinal: Int = 0 }
  def checked(checked:String): CheckedOrUnchecked2 = CheckedCase(checked)
  final case class RawCase(raw: String) extends CheckedOrUnchecked2 { final def _ordinal: Int = 1 }
  def raw(raw:String): CheckedOrUnchecked2 = RawCase(raw)

  object CheckedCase {
    val schema: Schema[CheckedCase] = bijection(string
    .addHints(
      Hints.empty
    )
    .validated(Pattern("^\\w+$")), CheckedCase(_), _.checked)
    val alt = schema.oneOf[CheckedOrUnchecked2]("checked")
  }
  object RawCase {
    val schema: Schema[RawCase] = bijection(string
    .addHints(
      Hints.empty
    )
    , RawCase(_), _.raw)
    val alt = schema.oneOf[CheckedOrUnchecked2]("raw")
  }

  implicit val schema: Schema[CheckedOrUnchecked2] = union(
    CheckedCase.alt,
    RawCase.alt,
  ){
    _._ordinal
  }
  .withId(ShapeId("smithy4s.example", "CheckedOrUnchecked2"))
  .addHints(
    Hints(
      Untagged(),
    )
  )
}
