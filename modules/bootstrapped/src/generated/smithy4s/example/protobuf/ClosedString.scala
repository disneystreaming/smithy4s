package smithy4s.example.protobuf

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.EnumTag
import smithy4s.schema.Schema.enumeration

sealed abstract class ClosedString(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = ClosedString
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = ClosedString
  @inline final def widen: ClosedString = this
}
object ClosedString extends Enumeration[ClosedString] with ShapeTag.Companion[ClosedString] {
  val id: ShapeId = ShapeId("smithy4s.example.protobuf", "ClosedString")

  val hints: Hints = Hints.empty

  case object FOO extends ClosedString("FOO", "FOO", 0, Hints.empty) {
    override val hints: Hints = Hints(alloy.proto.ProtoIndex(0)).lazily
  }
  case object BAR extends ClosedString("BAR", "BAR", 1, Hints.empty) {
    override val hints: Hints = Hints(alloy.proto.ProtoIndex(1)).lazily
  }

  val values: List[ClosedString] = List(
    FOO,
    BAR,
  )
  val tag: EnumTag[ClosedString] = EnumTag.ClosedStringEnum
  implicit val schema: Schema[ClosedString] = enumeration(tag, values).withId(id).addHints(hints)
}
