package smithy4s.example

import alloy.Untagged
import smithy.api.Pattern
import smithy4s.Bijection
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
  final case class RawCase(raw: String) extends CheckedOrUnchecked2 { final def _ordinal: Int = 1 }

  object CheckedCase {
    implicit val fromValue: Bijection[String, CheckedCase] = Bijection(CheckedCase(_), _.checked)
    implicit val toValue: Bijection[CheckedCase, String] = fromValue.swap
    val schema: Schema[CheckedCase] = bijection(string.validated(Pattern("^\\w+$")), fromValue).addHints()
  }
  object RawCase {
    implicit val fromValue: Bijection[String, RawCase] = Bijection(RawCase(_), _.raw)
    implicit val toValue: Bijection[RawCase, String] = fromValue.swap
    val schema: Schema[RawCase] = bijection(string, fromValue)
  }

  val checked = CheckedCase.schema.oneOf[CheckedOrUnchecked2]("checked")
  val raw = RawCase.schema.oneOf[CheckedOrUnchecked2]("raw")

  implicit val schema: Schema[CheckedOrUnchecked2] = union(
    checked,
    raw,
  ){
    _._ordinal
  }
  .withId(ShapeId("smithy4s.example", "CheckedOrUnchecked2"))
  .addHints(
    Untagged(),
  )
}
