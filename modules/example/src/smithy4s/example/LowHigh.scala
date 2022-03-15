package smithy4s.example

import smithy4s.schema.syntax._

sealed abstract class LowHigh(_value: String, _ordinal: Int) extends smithy4s.Enumeration.Value {
  override val value : String = _value
  override val ordinal: Int = _ordinal
  override val hints: smithy4s.Hints = smithy4s.Hints.empty
}
object LowHigh extends smithy4s.Enumeration[LowHigh] with smithy4s.ShapeTag.Companion[LowHigh] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "LowHigh")

  val hints : smithy4s.Hints = smithy4s.Hints.empty

  case object Low extends LowHigh("Low", 0)
  case object High extends LowHigh("High", 1)

  val values: List[LowHigh] = List(
    Low,
    High,
  )
  implicit val schema: smithy4s.Schema[LowHigh] = enumeration(values).withId(id).addHints(hints)
}