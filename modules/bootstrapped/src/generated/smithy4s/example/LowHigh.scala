package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.stringEnumeration

/** @param LOW
  *   low
  * @param HIGH
  *   high
  */
sealed abstract class LowHigh(_name: String, _stringValue: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = LowHigh
  override val name: String = _name
  override val stringValue: String = _stringValue
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = LowHigh
  @inline final def widen: LowHigh = this
}
object LowHigh extends Enumeration[LowHigh] with ShapeTag.Companion[LowHigh] {
  val id: ShapeId = ShapeId("smithy4s.example", "LowHigh")

  val hints: Hints = Hints.empty

  /** low */
  case object LOW extends LowHigh("LOW", "Low", 0, Hints.empty) {
    override val hints: Hints = Hints(smithy.api.Documentation("low")).lazily
  }
  /** high */
  case object HIGH extends LowHigh("HIGH", "High", 1, Hints.empty) {
    override val hints: Hints = Hints(smithy.api.Documentation("high")).lazily
  }

  val values: List[LowHigh] = List(
    LOW,
    HIGH,
  )
  implicit val schema: Schema[LowHigh] = stringEnumeration(values).withId(id).addHints(hints)
}
