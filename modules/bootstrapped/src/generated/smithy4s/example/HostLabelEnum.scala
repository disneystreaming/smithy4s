package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.stringEnumeration

sealed abstract class HostLabelEnum(_name: String, _stringValue: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = HostLabelEnum
  override val name: String = _name
  override val stringValue: String = _stringValue
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = HostLabelEnum
  @inline final def widen: HostLabelEnum = this
}
object HostLabelEnum extends Enumeration[HostLabelEnum] with ShapeTag.Companion[HostLabelEnum] {
  val id: ShapeId = ShapeId("smithy4s.example", "HostLabelEnum")

  val hints: Hints = Hints.empty

  case object THING1 extends HostLabelEnum("THING1", "THING1", 0, Hints())
  case object THING2 extends HostLabelEnum("THING2", "THING2", 1, Hints())

  val values: List[HostLabelEnum] = List(
    THING1,
    THING2,
  )
  implicit val schema: Schema[HostLabelEnum] = stringEnumeration(values).withId(id).addHints(hints)
}
