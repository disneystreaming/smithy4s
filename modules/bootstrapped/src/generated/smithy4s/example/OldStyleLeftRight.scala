package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.stringEnumeration

sealed abstract class OldStyleLeftRight(_name: String, _stringValue: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = OldStyleLeftRight
  override val name: String = _name
  override val stringValue: String = _stringValue
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = OldStyleLeftRight
  @inline final def widen: OldStyleLeftRight = this
}
object OldStyleLeftRight extends Enumeration[OldStyleLeftRight] with ShapeTag.Companion[OldStyleLeftRight] {
  val id: ShapeId = ShapeId("smithy4s.example", "oldStyleLeftRight")

  val hints: Hints = Hints.empty

  case object LEFT extends OldStyleLeftRight("LEFT", "left", 0, Hints())
  case object RIGHT extends OldStyleLeftRight("RIGHT", "right", 1, Hints())

  val values: List[OldStyleLeftRight] = List(
    LEFT,
    RIGHT,
  )
  implicit val schema: Schema[OldStyleLeftRight] = stringEnumeration(values).withId(id).addHints(hints)
}
