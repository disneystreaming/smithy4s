package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.stringEnumeration

sealed abstract class PizzaBase(_name: String, _stringValue: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = PizzaBase
  override val name: String = _name
  override val stringValue: String = _stringValue
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = PizzaBase
  @inline final def widen: PizzaBase = this
}
object PizzaBase extends Enumeration[PizzaBase] with ShapeTag.Companion[PizzaBase] {
  val id: ShapeId = ShapeId("smithy4s.example", "PizzaBase")

  val hints: Hints = Hints.empty

  case object CREAM extends PizzaBase("CREAM", "C", 0, Hints.empty)
  case object TOMATO extends PizzaBase("TOMATO", "T", 1, Hints.empty)

  val values: List[PizzaBase] = List(
    CREAM,
    TOMATO,
  )
  implicit val schema: Schema[PizzaBase] = stringEnumeration(values).withId(id).addHints(hints)
}
