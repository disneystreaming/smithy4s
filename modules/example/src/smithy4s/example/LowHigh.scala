package smithy4s.example

import smithy4s._
import smithy4s.schema.Schema._

sealed abstract class LowHigh(_value: String, _name: String, _ordinal: Int) extends Enumeration.Value {
  override val value: String = _value
  override val name: String = _name
  override val ordinal: Int = _ordinal
  override val hints: Hints = Hints.empty
  @inline final def widen: LowHigh = this
}
object LowHigh extends Enumeration[LowHigh] with ShapeTag.Companion[LowHigh] {
  val id: ShapeId = ShapeId("smithy4s.example", "LowHigh")
  
  val hints : Hints = Hints.empty
  
  case object LOW extends LowHigh("Low", "LOW", 0)
  case object HIGH extends LowHigh("High", "HIGH", 1)
  
  val values: List[LowHigh] = List(
    LOW,
    HIGH,
  )
  implicit val schema: Schema[LowHigh] = enumeration(values).withId(id).addHints(hints)
}