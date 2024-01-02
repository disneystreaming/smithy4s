package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.openStringEnumeration

sealed abstract class OpenNumsStr(_name: String, _stringValue: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = OpenNumsStr
  override val name: String = _name
  override val stringValue: String = _stringValue
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = OpenNumsStr
  @inline final def widen: OpenNumsStr = this
}
object OpenNumsStr extends Enumeration[OpenNumsStr] with ShapeTag.Companion[OpenNumsStr] {
  val id: ShapeId = ShapeId("smithy4s.example", "OpenNumsStr")

  val hints: Hints = Hints(
    alloy.OpenEnum(),
  )

  case object ONE extends OpenNumsStr("ONE", "ONE", 0, Hints())
  case object TWO extends OpenNumsStr("TWO", "TWO", 1, Hints())
  final case class $Unknown(str: String) extends OpenNumsStr(str, "$Unknown", -1, Hints.empty)

  val $unknown: String => OpenNumsStr = $Unknown(_)

  val values: List[OpenNumsStr] = List(
    ONE,
    TWO,
  )
  implicit val schema: Schema[OpenNumsStr] = openStringEnumeration(values, $unknown).withId(id).addHints(hints)
}
