package smithy4s.example

import smithy4s.Schema
import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.enumeration

sealed abstract class Letters(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  @inline final def widen: Letters = this
}
object Letters extends Enumeration[Letters] with ShapeTag.Companion[Letters] {
  val id: ShapeId = ShapeId("smithy4s.example", "Letters")

  val hints : Hints = Hints.empty

  case object A extends Letters("a", "A", 0, Hints(smithy.api.EnumValue(smithy4s.Document.fromString("a"))))
  case object B extends Letters("b", "B", 1, Hints(smithy.api.EnumValue(smithy4s.Document.fromString("b"))))
  case object C extends Letters("c", "C", 2, Hints(smithy.api.EnumValue(smithy4s.Document.fromString("c"))))

  val values: List[Letters] = List(
    A,
    B,
    C,
  )
  implicit val schema: Schema[Letters] = enumeration(values).withId(id).addHints(hints)
}