package smithy4s.example

import smithy4s.schema.Schema._

sealed abstract class Letters(_value: String, _name: String, _ordinal: Int) extends smithy4s.Enumeration.Value {
  override val value: String = _value
  override val name: String = _name
  override val ordinal: Int = _ordinal
  override val hints: smithy4s.Hints = smithy4s.Hints.empty
  @inline final def widen: Letters = this
}
object Letters extends smithy4s.Enumeration[Letters] with smithy4s.ShapeTag.Companion[Letters] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "Letters")

  val hints : smithy4s.Hints = smithy4s.Hints.empty

  case object A extends Letters("a", "A", 0)
  case object B extends Letters("b", "B", 1)
  case object C extends Letters("c", "C", 2)

  val values: List[Letters] = List(
    A,
    B,
    C,
  )
  implicit val schema: smithy4s.Schema[Letters] = enumeration(values).withId(id).addHints(hints)
}