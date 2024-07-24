package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.stringEnumeration

sealed abstract class SwitchState(_name: String, _stringValue: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = SwitchState
  override val name: String = _name
  override val stringValue: String = _stringValue
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = SwitchState
  @inline final def widen: SwitchState = this
}
object SwitchState extends Enumeration[SwitchState] with ShapeTag.Companion[SwitchState] {
  val id: ShapeId = ShapeId("smithy4s.example", "SwitchState")

  val hints: Hints = Hints.empty

  case object ON extends SwitchState("ON", "ON", 0, Hints.empty)
  case object OFF extends SwitchState("OFF", "OFF", 1, Hints.empty)

  val values: List[SwitchState] = List(
    ON,
    OFF,
  )
  implicit val schema: Schema[SwitchState] = stringEnumeration(values).withId(id).addHints(hints)
}
