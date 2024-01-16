package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.EnumTag
import smithy4s.schema.Schema.enumeration

sealed abstract class HostLabelEnum(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = HostLabelEnum
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = HostLabelEnum
  @inline final def widen: HostLabelEnum = this
}
object HostLabelEnum extends Enumeration[HostLabelEnum] with ShapeTag.Companion[HostLabelEnum] {
  val id: ShapeId = ShapeId("smithy4s.example", "HostLabelEnum")

  val hints: Hints = Hints.empty

  case object THING1 extends HostLabelEnum("THING1", "THING1", 0, Hints.empty)
  case object THING2 extends HostLabelEnum("THING2", "THING2", 1, Hints.empty)

  val values: List[HostLabelEnum] = List(
    THING1,
    THING2,
  )
  val tag: EnumTag[HostLabelEnum] = EnumTag.ClosedStringEnum
  implicit val schema: Schema[HostLabelEnum] = enumeration(tag, values).withId(id).addHints(hints)
}
