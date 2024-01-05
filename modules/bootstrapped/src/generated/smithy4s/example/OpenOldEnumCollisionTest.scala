package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.EnumTag
import smithy4s.schema.Schema.enumeration

sealed abstract class OpenOldEnumCollisionTest(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = OpenOldEnumCollisionTest
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = OpenOldEnumCollisionTest
  @inline final def widen: OpenOldEnumCollisionTest = this
}
object OpenOldEnumCollisionTest extends Enumeration[OpenOldEnumCollisionTest] with ShapeTag.Companion[OpenOldEnumCollisionTest] {
  val id: ShapeId = ShapeId("smithy4s.example", "OpenOldEnumCollisionTest")

  val hints: Hints = Hints.lazily(
    Hints(
      alloy.OpenEnum(),
    )
  )

  case object Unknown extends OpenOldEnumCollisionTest("unknown", "Unknown", 0, Hints.empty)
  final case class $Unknown(str: String) extends OpenOldEnumCollisionTest(str, "$Unknown", -1, Hints.empty)

  val $unknown: String => OpenOldEnumCollisionTest = $Unknown(_)

  val values: List[OpenOldEnumCollisionTest] = List(
    Unknown,
  )
  val tag: EnumTag[OpenOldEnumCollisionTest] = EnumTag.OpenStringEnum($unknown)
  implicit val schema: Schema[OpenOldEnumCollisionTest] = enumeration(tag, values).withId(id).addHints(hints)
}
