package smithy4s.example.protobuf

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.intEnumeration

sealed abstract class ClosedInt(_name: String, _stringValue: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = ClosedInt
  override val name: String = _name
  override val stringValue: String = _stringValue
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = ClosedInt
  @inline final def widen: ClosedInt = this
}
object ClosedInt extends Enumeration[ClosedInt] with ShapeTag.Companion[ClosedInt] {
  val id: ShapeId = ShapeId("smithy4s.example.protobuf", "ClosedInt")

  val hints: Hints = Hints.empty

  case object FOO extends ClosedInt("FOO", "FOO", 0, Hints.empty) {
    override val hints: Hints = Hints(alloy.proto.ProtoIndex(0)).lazily
  }
  case object BAR extends ClosedInt("BAR", "BAR", 1, Hints.empty) {
    override val hints: Hints = Hints(alloy.proto.ProtoIndex(1)).lazily
  }

  val values: List[ClosedInt] = List(
    FOO,
    BAR,
  )
  implicit val schema: Schema[ClosedInt] = intEnumeration(values).withId(id).addHints(hints)
}
