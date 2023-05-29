package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.EnumTag
import smithy4s.schema.Schema.enumeration

/** @param LOW
  *   low
  * @param HIGH
  *   high
  */
sealed abstract class LowHigh(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = LowHigh
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = LowHigh
  @inline final def widen: LowHigh = this
}
object LowHigh extends Enumeration[LowHigh] with ShapeTag.Companion[LowHigh] {
  val id: ShapeId = ShapeId("smithy4s.example", "LowHigh")

  val hints: Hints = Hints(
    smithy.api.Enum(List(smithy.api.EnumDefinition(value = smithy.api.NonEmptyString("Low"), name = Some(smithy.api.EnumConstantBodyName("LOW")), documentation = Some("low"), tags = None, deprecated = None), smithy.api.EnumDefinition(value = smithy.api.NonEmptyString("High"), name = Some(smithy.api.EnumConstantBodyName("HIGH")), documentation = Some("high"), tags = None, deprecated = None))),
  )

  /** low */
  case object LOW extends LowHigh("Low", "LOW", 0, Hints(smithy.api.Documentation("low")))
  /** high */
  case object HIGH extends LowHigh("High", "HIGH", 1, Hints(smithy.api.Documentation("high")))

  val values: List[LowHigh] = List(
    LOW,
    HIGH,
  )
  val tag: EnumTag = EnumTag.StringEnum
  implicit val schema: Schema[LowHigh] = enumeration(tag, values).withId(id).addHints(hints)
}
