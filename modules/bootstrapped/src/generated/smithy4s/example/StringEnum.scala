package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.stringEnumeration

sealed abstract class StringEnum(_name: String, _stringValue: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = StringEnum
  override val name: String = _name
  override val stringValue: String = _stringValue
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = StringEnum
  @inline final def widen: StringEnum = this
}
object StringEnum extends Enumeration[StringEnum] with ShapeTag.Companion[StringEnum] {
  val id: ShapeId = ShapeId("smithy4s.example", "StringEnum")

  val hints: Hints = Hints.empty

  case object STRING extends StringEnum("STRING", "string", 0, Hints())
  case object INTERESTING extends StringEnum("INTERESTING", "interesting", 1, Hints())

  val values: List[StringEnum] = List(
    STRING,
    INTERESTING,
  )
  implicit val schema: Schema[StringEnum] = stringEnumeration(values).withId(id).addHints(hints)
}
