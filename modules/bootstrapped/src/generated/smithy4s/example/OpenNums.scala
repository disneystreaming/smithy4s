package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.openIntEnumeration

sealed abstract class OpenNums(_name: String, _stringValue: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = OpenNums
  override val name: String = _name
  override val stringValue: String = _stringValue
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = OpenNums
  @inline final def widen: OpenNums = this
}
object OpenNums extends Enumeration[OpenNums] with ShapeTag.Companion[OpenNums] {
  val id: ShapeId = ShapeId("smithy4s.example", "OpenNums")

  val hints: Hints = Hints(
    alloy.OpenEnum(),
  ).lazily

  case object ONE extends OpenNums("ONE", "ONE", 1, Hints.empty)
  case object TWO extends OpenNums("TWO", "TWO", 2, Hints.empty)
  final case class $Unknown(int: Int) extends OpenNums("$Unknown", "$Unknown", int, Hints.empty)

  val $unknown: Int => OpenNums = $Unknown(_)

  val values: List[OpenNums] = List(
    ONE,
    TWO,
  )
  implicit val schema: Schema[OpenNums] = openIntEnumeration(values, $unknown).withId(id).addHints(hints)
}
