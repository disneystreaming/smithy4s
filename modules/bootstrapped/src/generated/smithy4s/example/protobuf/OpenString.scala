package smithy4s.example.protobuf

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.openStringEnumeration

sealed abstract class OpenString(_name: String, _stringValue: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = OpenString
  override val name: String = _name
  override val stringValue: String = _stringValue
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = OpenString
  @inline final def widen: OpenString = this
}
object OpenString extends Enumeration[OpenString] with ShapeTag.Companion[OpenString] {
  val id: ShapeId = ShapeId("smithy4s.example.protobuf", "OpenString")

  val hints: Hints = Hints(
    alloy.OpenEnum(),
  ).lazily

  case object FOO extends OpenString("FOO", "FOO", 0, Hints.empty)
  case object BAR extends OpenString("BAR", "BAR", 1, Hints.empty)
  final case class $Unknown(str: String) extends OpenString("$Unknown", str, -1, Hints.empty)

  val $unknown: String => OpenString = $Unknown(_)

  val values: List[OpenString] = List(
    FOO,
    BAR,
  )
  implicit val schema: Schema[OpenString] = openStringEnumeration(values, $unknown).withId(id).addHints(hints)
}
