package smithy4s.example

import smithy4s.schema.Schema._

sealed abstract class LowHigh(_value: String, _name: String, _ordinal: Int) extends smithy4s.Enumeration.Value {
  override val value: String = _value
  override val name: String = _name
  override val ordinal: Int = _ordinal
  override val hints: smithy4s.Hints = smithy4s.Hints.empty
  @inline final def widen: LowHigh = this
}
object LowHigh extends smithy4s.Enumeration[LowHigh] with smithy4s.ShapeTag.Companion[LowHigh] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "LowHigh")

  val hints : smithy4s.Hints = smithy4s.Hints.empty

  case object LOW extends LowHigh("Low", "LOW", 0)
  case object HIGH extends LowHigh("High", "HIGH", 1)

  val values: List[LowHigh] = List(
    LOW,
    HIGH,
  )
  implicit val schema: smithy4s.Schema[LowHigh] = enumeration(values).withId(id).addHints(hints)
}