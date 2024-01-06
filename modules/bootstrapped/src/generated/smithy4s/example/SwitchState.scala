package smithy4s.example

import _root_.smithy4s.Enumeration
import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.EnumTag
import _root_.smithy4s.schema.Schema.enumeration

sealed abstract class SwitchState(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = SwitchState
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = SwitchState
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
  val tag: EnumTag[SwitchState] = EnumTag.ClosedStringEnum
  implicit val schema: Schema[SwitchState] = enumeration(tag, values).withId(id).addHints(hints)
}
