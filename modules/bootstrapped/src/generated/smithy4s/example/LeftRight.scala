package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.stringEnumeration

sealed abstract class LeftRight(_name: String, _stringValue: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = LeftRight
  override val name: String = _name
  override val stringValue: String = _stringValue
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = LeftRight
  @inline final def widen: LeftRight = this
}
object LeftRight extends Enumeration[LeftRight] with ShapeTag.Companion[LeftRight] {
  val id: ShapeId = ShapeId("smithy4s.example", "leftRight")

  val hints: Hints = Hints.empty

  case object LEFT extends LeftRight("LEFT", "left", 0, Hints.empty)
  case object RIGHT extends LeftRight("RIGHT", "right", 1, Hints.empty)

  val values: List[LeftRight] = List(
    LEFT,
    RIGHT,
  )
  implicit val schema: Schema[LeftRight] = stringEnumeration(values).withId(id).addHints(hints)
}
