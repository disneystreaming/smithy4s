package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.EnumTag
import smithy4s.schema.Schema.enumeration

sealed abstract class OpenOldEnumTest(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = OpenOldEnumTest
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = OpenOldEnumTest
  @inline final def widen: OpenOldEnumTest = this
}
object OpenOldEnumTest extends Enumeration[OpenOldEnumTest] with ShapeTag.Companion[OpenOldEnumTest] {
  val id: ShapeId = ShapeId("smithy4s.example", "OpenOldEnumTest")

  val hints: Hints = Hints.lazily(
    Hints(
      alloy.OpenEnum(),
    )
  )

  case object ONE extends OpenOldEnumTest("ONE", "ONE", 0, Hints.empty)
  final case class $Unknown(str: String) extends OpenOldEnumTest(str, "$Unknown", -1, Hints.empty)

  val $unknown: String => OpenOldEnumTest = $Unknown(_)

  val values: List[OpenOldEnumTest] = List(
    ONE,
  )
  val tag: EnumTag[OpenOldEnumTest] = EnumTag.OpenStringEnum($unknown)
  implicit val schema: Schema[OpenOldEnumTest] = enumeration(tag, values).withId(id).addHints(hints)
}
