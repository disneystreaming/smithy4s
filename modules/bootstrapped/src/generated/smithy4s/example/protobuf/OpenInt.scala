package smithy4s.example.protobuf

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.openIntEnumeration

sealed abstract class OpenInt(_name: String, _stringValue: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = OpenInt
  override val name: String = _name
  override val stringValue: String = _stringValue
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = OpenInt
  @inline final def widen: OpenInt = this
}
object OpenInt extends Enumeration[OpenInt] with ShapeTag.Companion[OpenInt] {
  val id: ShapeId = ShapeId("smithy4s.example.protobuf", "OpenInt")

  val hints: Hints = Hints(
    alloy.OpenEnum(),
  ).lazily

  case object FOO extends OpenInt("FOO", "FOO", 0, Hints.empty)
  case object BAR extends OpenInt("BAR", "BAR", 1, Hints.empty)
  final case class $Unknown(int: Int) extends OpenInt("$Unknown", "$Unknown", int, Hints.empty)

  val $unknown: Int => OpenInt = $Unknown(_)

  val values: List[OpenInt] = List(
    FOO,
    BAR,
  )
  implicit val schema: Schema[OpenInt] = openIntEnumeration(values, $unknown).withId(id).addHints(hints)
}
