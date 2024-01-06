package smithy4s.example

import _root_.smithy4s.Enumeration
import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.EnumTag
import _root_.smithy4s.schema.Schema.enumeration

sealed abstract class TheEnum(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = TheEnum
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = TheEnum
  @inline final def widen: TheEnum = this
}
object TheEnum extends Enumeration[TheEnum] with ShapeTag.Companion[TheEnum] {
  val id: ShapeId = ShapeId("smithy4s.example", "TheEnum")

  val hints: Hints = Hints.empty

  case object V1 extends TheEnum("v1", "V1", 0, Hints())
  case object V2 extends TheEnum("v2", "V2", 1, Hints())

  val values: List[TheEnum] = List(
    V1,
    V2,
  )
  val tag: EnumTag[TheEnum] = EnumTag.ClosedStringEnum
  implicit val schema: Schema[TheEnum] = enumeration(tag, values).withId(id).addHints(hints)
}
