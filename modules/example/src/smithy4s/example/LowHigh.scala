package smithy4s.example

import smithy4s.Schema
import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.enumeration

sealed abstract class LowHigh(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  @inline final def widen: LowHigh = this
}
object LowHigh extends Enumeration[LowHigh] with ShapeTag.Companion[LowHigh] {
  val id: ShapeId = ShapeId("smithy4s.example", "LowHigh")

  val hints : Hints = Hints(
    smithy.api.Enum(List(smithy.api.EnumDefinition(value = smithy.api.NonEmptyString("Low"), name = Some(smithy.api.EnumConstantBodyName("LOW")), documentation = None, tags = None, deprecated = None), smithy.api.EnumDefinition(value = smithy.api.NonEmptyString("High"), name = Some(smithy.api.EnumConstantBodyName("HIGH")), documentation = None, tags = None, deprecated = None))),
  )

  case object LOW extends LowHigh("Low", "LOW", 0, Hints())
  case object HIGH extends LowHigh("High", "HIGH", 1, Hints())

  val values: List[LowHigh] = List(
    LOW,
    HIGH,
  )
  implicit val schema: Schema[LowHigh] = enumeration(values).withId(id).addHints(hints)
}