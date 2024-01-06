package smithy4s.example.collision

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.EnumTag
import smithy4s.schema.Schema.enumeration

sealed abstract class KeywordEnum(_value: java.lang.String, _name: java.lang.String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = KeywordEnum
  override val value: java.lang.String = _value
  override val name: java.lang.String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = KeywordEnum
  @inline final def widen: KeywordEnum = this
}
object KeywordEnum extends Enumeration[KeywordEnum] with ShapeTag.Companion[KeywordEnum] {
  val id: ShapeId = ShapeId("smithy4s.example.collision", "KeywordEnum")

  val hints: Hints = Hints.empty

  case object _implicit extends KeywordEnum("implicit", "implicit", 0, Hints())

  val values: List[KeywordEnum] = List(
    _implicit,
  )
  val tag: EnumTag[KeywordEnum] = EnumTag.ClosedStringEnum
  implicit val schema: Schema[KeywordEnum] = enumeration(tag, values).withId(id).addHints(hints)
}
