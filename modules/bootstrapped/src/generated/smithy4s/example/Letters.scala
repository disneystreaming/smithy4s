package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.EnumTag
import smithy4s.schema.Schema.enumeration

sealed abstract class Letters(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = Letters
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = Letters
  @inline final def widen: Letters = this
}
object Letters extends Enumeration[Letters] with ShapeTag.Companion[Letters] {
  case object A extends Letters("a", "A", 0, Hints())
  case object B extends Letters("b", "B", 1, Hints())
  case object C extends Letters("c", "C", 2, Hints())

  val values: List[Letters] = List(
    A,
    B,
    C,
  )
  val tag: EnumTag = EnumTag.StringEnum
  implicit val schema: Schema[Letters] = enumeration(tag, values)
  .withId(ShapeId("smithy4s.example", "Letters"))
  .addHints(
    Hints.empty
  )
}
