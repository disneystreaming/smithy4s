package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.openStringEnumeration

sealed abstract class OpenOldEnumTest(_name: String, _stringValue: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = OpenOldEnumTest
  override val name: String = _name
  override val stringValue: String = _stringValue
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = OpenOldEnumTest
  @inline final def widen: OpenOldEnumTest = this
}
object OpenOldEnumTest extends Enumeration[OpenOldEnumTest] with ShapeTag.Companion[OpenOldEnumTest] {
  val id: ShapeId = ShapeId("smithy4s.example", "OpenOldEnumTest")

  val hints: Hints = Hints(
    alloy.OpenEnum(),
  ).lazily

  case object ONE extends OpenOldEnumTest("ONE", "ONE", 0, Hints.empty)
  final case class $Unknown(str: String) extends OpenOldEnumTest("$Unknown", str, -1, Hints.empty)

  val $unknown: String => OpenOldEnumTest = $Unknown(_)

  val values: List[OpenOldEnumTest] = List(
    ONE,
  )
  implicit val schema: Schema[OpenOldEnumTest] = openStringEnumeration(values, $unknown).withId(id).addHints(hints)
}
