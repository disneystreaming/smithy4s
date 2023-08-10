package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.EnumTag
import smithy4s.schema.Schema.enumeration

sealed abstract class LeftRight(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = LeftRight
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = LeftRight
  @inline final def widen: LeftRight = this
}
object LeftRight extends Enumeration[LeftRight] with ShapeTag.Companion[LeftRight] {
  val id: ShapeId = ShapeId("smithy4s.example", "leftRight")

  val hints: Hints = Hints.empty

  case object LEFT extends LeftRight("left", "LEFT", 0, Hints())
  case object RIGHT extends LeftRight("right", "RIGHT", 1, Hints())

  val values: List[LeftRight] = List(
    LEFT,
    RIGHT,
  )
  val tag: EnumTag[LeftRight] = EnumTag.ClosedStringEnum
  implicit val schema: Schema[LeftRight] = enumeration(tag, values).withId(id).addHints(hints)
}
