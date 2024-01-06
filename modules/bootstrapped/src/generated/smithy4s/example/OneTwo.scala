package smithy4s.example

import _root_.smithy4s.Enumeration
import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.EnumTag
import _root_.smithy4s.schema.Schema.enumeration

sealed abstract class OneTwo(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = OneTwo
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = OneTwo
  @inline final def widen: OneTwo = this
}
object OneTwo extends Enumeration[OneTwo] with ShapeTag.Companion[OneTwo] {
  val id: ShapeId = ShapeId("smithy4s.example", "oneTwo")

  val hints: Hints = Hints.empty

  case object ONE extends OneTwo("ONE", "ONE", 1, Hints())
  case object TWO extends OneTwo("TWO", "TWO", 2, Hints())

  val values: List[OneTwo] = List(
    ONE,
    TWO,
  )
  val tag: EnumTag[OneTwo] = EnumTag.ClosedIntEnum
  implicit val schema: Schema[OneTwo] = enumeration(tag, values).withId(id).addHints(hints)
}
