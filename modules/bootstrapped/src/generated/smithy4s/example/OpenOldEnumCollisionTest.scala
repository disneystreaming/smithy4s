package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.openStringEnumeration

sealed abstract class OpenOldEnumCollisionTest(_name: String, _stringValue: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = OpenOldEnumCollisionTest
  override val name: String = _name
  override val stringValue: String = _stringValue
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = OpenOldEnumCollisionTest
  @inline final def widen: OpenOldEnumCollisionTest = this
}
object OpenOldEnumCollisionTest extends Enumeration[OpenOldEnumCollisionTest] with ShapeTag.Companion[OpenOldEnumCollisionTest] {
  val id: ShapeId = ShapeId("smithy4s.example", "OpenOldEnumCollisionTest")

  val hints: Hints = Hints(
    alloy.OpenEnum(),
  )

  case object Unknown extends OpenOldEnumCollisionTest("Unknown", "unknown", 0, Hints())
  final case class $Unknown(str: String) extends OpenOldEnumCollisionTest(str, "$Unknown", -1, Hints.empty)

  val $unknown: String => OpenOldEnumCollisionTest = $Unknown(_)

  val values: List[OpenOldEnumCollisionTest] = List(
    Unknown,
  )
  implicit val schema: Schema[OpenOldEnumCollisionTest] = openStringEnumeration(values, $unknown).withId(id).addHints(hints)
}
