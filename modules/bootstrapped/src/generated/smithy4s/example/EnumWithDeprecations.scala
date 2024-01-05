package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.EnumTag
import smithy4s.schema.Schema.enumeration

/** some docs here */
@deprecated(message = "N/A", since = "N/A")
sealed abstract class EnumWithDeprecations(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = EnumWithDeprecations
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = EnumWithDeprecations
  @inline final def widen: EnumWithDeprecations = this
}
object EnumWithDeprecations extends Enumeration[EnumWithDeprecations] with ShapeTag.Companion[EnumWithDeprecations] {
  val id: ShapeId = ShapeId("smithy4s.example", "EnumWithDeprecations")

  val hints: Hints = Hints(
    smithy.api.Documentation("some docs here"),
    smithy.api.Deprecated(message = None, since = None),
  )

  @deprecated(message = "N/A", since = "N/A")
  case object OLD extends EnumWithDeprecations("OLD", "OLD", 0, Hints()) {
    override val hints: Hints = Hints(smithy.api.Deprecated(message = None, since = None))
  }
  case object NEW extends EnumWithDeprecations("NEW", "NEW", 1, Hints())

  val values: List[EnumWithDeprecations] = List(
    OLD,
    NEW,
  )
  val tag: EnumTag[EnumWithDeprecations] = EnumTag.ClosedStringEnum
  implicit val schema: Schema[EnumWithDeprecations] = enumeration(tag, values).withId(id).addHints(hints)
}
