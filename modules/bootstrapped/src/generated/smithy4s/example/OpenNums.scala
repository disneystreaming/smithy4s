package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.EnumTag
import smithy4s.schema.Schema.enumeration

sealed abstract class OpenNums(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = OpenNums
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = OpenNums
  @inline final def widen: OpenNums = this
}
object OpenNums extends Enumeration[OpenNums] with ShapeTag.Companion[OpenNums] {
  val id: ShapeId = ShapeId("smithy4s.example", "OpenNums")

  val hints: Hints = Hints(
    alloy.OpenEnum(),
  )

  case object ONE extends OpenNums("ONE", "ONE", 1, Hints())
  case object TWO extends OpenNums("TWO", "TWO", 2, Hints())
  final case class $Unknown(int: Int) extends OpenNums("$Unknown", "$Unknown", int, Hints.empty)

  val $unknown: Int => OpenNums = $Unknown(_)

  val values: List[OpenNums] = List(
    ONE,
    TWO,
  )
  val tag: EnumTag[OpenNums] = EnumTag.OpenIntEnum($unknown)
  implicit val schema: Schema[OpenNums] = enumeration(tag, values).withId(id).addHints(hints)
}
