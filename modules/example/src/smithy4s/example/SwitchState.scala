package smithy4s.example

import smithy4s.schema.Schema._

sealed abstract class SwitchState(_value: String, _name: String, _ordinal: Int) extends smithy4s.Enumeration.Value {
  override val value: String = _value
  override val name: String = _name
  override val ordinal: Int = _ordinal
  override val hints: smithy4s.Hints = smithy4s.Hints.empty
  @inline final def widen: SwitchState = this
}
object SwitchState extends smithy4s.Enumeration[SwitchState] with smithy4s.ShapeTag.Companion[SwitchState] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "SwitchState")

  val hints : smithy4s.Hints = smithy4s.Hints.empty

  case object ON extends SwitchState("ON", "ON", 0)
  case object OFF extends SwitchState("OFF", "OFF", 1)

  val values: List[SwitchState] = List(
    ON,
    OFF,
  )
  implicit val schema: smithy4s.Schema[SwitchState] = enumeration(values).withId(id).addHints(hints)
}