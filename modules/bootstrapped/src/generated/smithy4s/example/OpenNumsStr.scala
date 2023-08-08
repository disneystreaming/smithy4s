package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.EnumTag
import smithy4s.schema.Schema.enumeration

sealed abstract class OpenNumsStr(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = OpenNumsStr
  override val value: String = _value
  override val name: String = _name
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
  final case class $Unknown(str: String) extends OpenNumsStr(str, s"$$Unknown", -1, Hints.empty)

  val $unknown: String => OpenNumsStr = $Unknown(_)

  val values: List[OpenNumsStr] = List(
    ONE,
    TWO,
  )
  val tag: EnumTag[OpenNumsStr] = EnumTag.OpenStringEnum($unknown)
  implicit val schema: Schema[OpenNumsStr] = enumeration(tag, values).withId(id).addHints(hints)
}
