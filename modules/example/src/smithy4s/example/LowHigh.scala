package smithy4s.example

import smithy4s.Schema
import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.enumeration

sealed abstract class LowHigh(_value: String, _name: String, _ordinal: Int) extends Enumeration.Value {
  override val value: String = _value
  override val name: String = _name
  override val ordinal: Int = _ordinal
  override val hints: Hints = Hints.empty
  @inline final def widen: LowHigh = this
}
object LowHigh extends Enumeration[LowHigh] with ShapeTag.Companion[LowHigh] {
  val id: ShapeId = ShapeId("smithy4s.example", "LowHigh")
  
  val hints : Hints = Hints(
    smithy.api.Enum(List(smithy.api.EnumDefinition(smithy.api.NonEmptyString("Low"), Some(smithy.api.EnumConstantBodyName("LOW")), None, None, None), smithy.api.EnumDefinition(smithy.api.NonEmptyString("High"), Some(smithy.api.EnumConstantBodyName("HIGH")), None, None, None))),
  )
  
  case object LOW extends LowHigh("Low", "LOW", 0)
  case object HIGH extends LowHigh("High", "HIGH", 1)
  
  val values: List[LowHigh] = List(
    LOW,
    HIGH,
  )
  implicit val schema: Schema[LowHigh] = enumeration(values).withId(id).addHints(hints)
}