package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.enumeration

sealed abstract class SwitchState(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  @inline final def widen: SwitchState = this
}
object SwitchState extends Enumeration[SwitchState] with ShapeTag.Companion[SwitchState] {
  val id: ShapeId = ShapeId("smithy4s.example", "SwitchState")

  val hints: Hints = Hints.empty

  case object ON extends SwitchState("ON", "ON", 0, Hints())
  case object OFF extends SwitchState("OFF", "OFF", 1, Hints())

  val values: List[SwitchState] = List(
    ON,
    OFF,
  )
  implicit val schema: Schema[SwitchState] = enumeration(values).withId(id).addHints(hints)
}