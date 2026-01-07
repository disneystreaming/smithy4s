package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.stringEnumeration

sealed abstract class Letters(_name: String, _stringValue: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = Letters
  override val name: String = _name
  override val stringValue: String = _stringValue
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = Letters
  @inline final def widen: Letters = this
}
object Letters extends Enumeration[Letters] with ShapeTag.Companion[Letters] {
  val id: ShapeId = ShapeId("smithy4s.example", "Letters")

  val hints: Hints = Hints.empty

  case object A extends Letters("A", "a", 0, Hints.empty)
  case object B extends Letters("B", "b", 1, Hints.empty)
  case object C extends Letters("C", "c", 2, Hints.empty)

  val values: List[Letters] = List(
    A,
    B,
    C,
  )
  implicit val schema: Schema[Letters] = stringEnumeration(values).withId(id).addHints(hints)
}
