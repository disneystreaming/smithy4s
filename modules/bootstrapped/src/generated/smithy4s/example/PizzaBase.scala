package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.EnumTag
import smithy4s.schema.Schema.enumeration

sealed abstract class PizzaBase(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = PizzaBase
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = PizzaBase
  @inline final def widen: PizzaBase = this
}
object PizzaBase extends Enumeration[PizzaBase] with ShapeTag.Companion[PizzaBase] {
  val hints: Hints = Hints.empty

  case object CREAM extends PizzaBase("C", "CREAM", 0, Hints())
  case object TOMATO extends PizzaBase("T", "TOMATO", 1, Hints())

  val values: List[PizzaBase] = List(
    CREAM,
    TOMATO,
  )
  val tag: EnumTag = EnumTag.StringEnum
  implicit val schema: Schema[PizzaBase] = enumeration(tag, values).withId(ShapeId("smithy4s.example", "PizzaBase")).addHints(hints)
}
