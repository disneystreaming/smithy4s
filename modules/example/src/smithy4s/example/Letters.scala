package smithy4s.example

import smithy4s._
import smithy4s.schema.Schema._

sealed abstract class Letters(_value: String, _name: String, _ordinal: Int) extends Enumeration.Value {
  override val value: String = _value
  override val name: String = _name
  override val ordinal: Int = _ordinal
  override val hints: Hints = Hints.empty
  @inline final def widen: Letters = this
}
object Letters extends Enumeration[Letters] with ShapeTag.Companion[Letters] {
  val id: ShapeId = ShapeId("smithy4s.example", "Letters")
  
  val hints : Hints = Hints.empty
  
  case object A extends Letters("a", "A", 0)
  case object B extends Letters("b", "B", 1)
  case object C extends Letters("c", "C", 2)
  
  val values: List[Letters] = List(
    A,
    B,
    C,
  )
  implicit val schema: Schema[Letters] = enumeration(values).withId(id).addHints(hints)
}