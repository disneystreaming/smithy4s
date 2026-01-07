package smithy4s.example.protobuf

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.stringEnumeration

sealed abstract class ClosedString(_name: String, _stringValue: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = ClosedString
  override val name: String = _name
  override val stringValue: String = _stringValue
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = ClosedString
  @inline final def widen: ClosedString = this
}
object ClosedString extends Enumeration[ClosedString] with ShapeTag.Companion[ClosedString] {
  val id: ShapeId = ShapeId("smithy4s.example.protobuf", "ClosedString")

  val hints: Hints = Hints.empty

  case object FOO extends ClosedString("FOO", "FOO", 0, Hints.empty)
  case object BAR extends ClosedString("BAR", "BAR", 1, Hints.empty)

  val values: List[ClosedString] = List(
    FOO,
    BAR,
  )
  implicit val schema: Schema[ClosedString] = stringEnumeration(values).withId(id).addHints(hints)
}
