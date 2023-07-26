package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.EnumTag
import smithy4s.schema.Schema.enumeration

sealed abstract class StringEnum(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = StringEnum
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = StringEnum
  @inline final def widen: StringEnum = this
}
object StringEnum extends Enumeration[StringEnum] with ShapeTag.Companion[StringEnum] {
  case object STRING extends StringEnum("string", "STRING", 0, Hints())
  case object INTERESTING extends StringEnum("interesting", "INTERESTING", 1, Hints())

  val values: List[StringEnum] = List(
    STRING,
    INTERESTING,
  )
  val tag: EnumTag = EnumTag.StringEnum
  implicit val schema: Schema[StringEnum] = enumeration(tag, values)
  .withId(ShapeId("smithy4s.example", "StringEnum"))
}
