package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.EnumTag
import smithy4s.schema.Schema.enumeration

sealed abstract class Ingredient(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = Ingredient
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = Ingredient
  @inline final def widen: Ingredient = this
}
object Ingredient extends Enumeration[Ingredient] with ShapeTag.Companion[Ingredient] {
  val id: ShapeId = ShapeId("smithy4s.example", "Ingredient")

  val hints: Hints = Hints.empty

  case object MUSHROOM extends Ingredient("Mushroom", "MUSHROOM", 0, Hints())
  case object CHEESE extends Ingredient("Cheese", "CHEESE", 1, Hints())
  case object SALAD extends Ingredient("Salad", "SALAD", 2, Hints())
  case object TOMATO extends Ingredient("Tomato", "TOMATO", 3, Hints())

  val values: List[Ingredient] = List(
    MUSHROOM,
    CHEESE,
    SALAD,
    TOMATO,
  )
  val tag: EnumTag = EnumTag.StringEnum
  implicit val schema: Schema[Ingredient] = enumeration(tag, values).withId(id).addHints(hints)
}
