package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.stringEnumeration

sealed abstract class Ingredient(_name: String, _stringValue: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = Ingredient
  override val name: String = _name
  override val stringValue: String = _stringValue
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = Ingredient
  @inline final def widen: Ingredient = this
}
object Ingredient extends Enumeration[Ingredient] with ShapeTag.Companion[Ingredient] {
  val id: ShapeId = ShapeId("smithy4s.example", "Ingredient")

  val hints: Hints = Hints.empty

  case object MUSHROOM extends Ingredient("MUSHROOM", "Mushroom", 0, Hints.empty)
  case object CHEESE extends Ingredient("CHEESE", "Cheese", 1, Hints.empty)
  case object SALAD extends Ingredient("SALAD", "Salad", 2, Hints.empty)
  case object TOMATO extends Ingredient("TOMATO", "Tomato", 3, Hints.empty)

  val values: List[Ingredient] = List(
    MUSHROOM,
    CHEESE,
    SALAD,
    TOMATO,
  )
  implicit val schema: Schema[Ingredient] = stringEnumeration(values).withId(id).addHints(hints)
}
