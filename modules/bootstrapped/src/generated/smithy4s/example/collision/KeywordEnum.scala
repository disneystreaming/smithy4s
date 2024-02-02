package smithy4s.example.collision

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.stringEnumeration

sealed abstract class KeywordEnum(_name: java.lang.String, _stringValue: java.lang.String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = KeywordEnum
  override val name: java.lang.String = _name
  override val stringValue: java.lang.String = _stringValue
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = KeywordEnum
  @inline final def widen: KeywordEnum = this
}
object KeywordEnum extends Enumeration[KeywordEnum] with ShapeTag.Companion[KeywordEnum] {
  val id: ShapeId = ShapeId("smithy4s.example.collision", "KeywordEnum")

  val hints: Hints = Hints.empty

  case object _implicit extends KeywordEnum("implicit", "implicit", 0, Hints.empty)
  case object _package extends KeywordEnum("package", "class", 1, Hints.empty)

  val values: List[KeywordEnum] = List(
    _implicit,
    _package,
  )
  implicit val schema: Schema[KeywordEnum] = stringEnumeration(values).withId(id).addHints(hints)
}
