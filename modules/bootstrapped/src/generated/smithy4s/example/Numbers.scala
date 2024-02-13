package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.intEnumeration

sealed abstract class Numbers(_name: String, _stringValue: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = Numbers
  override val name: String = _name
  override val stringValue: String = _stringValue
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = Numbers
  @inline final def widen: Numbers = this
}
object Numbers extends Enumeration[Numbers] with ShapeTag.Companion[Numbers] {
  val id: ShapeId = ShapeId("smithy4s.example", "Numbers")

  val hints: Hints = Hints.empty

  case object ONE extends Numbers("ONE", "ONE", 1, Hints.empty)
  case object TWO extends Numbers("TWO", "TWO", 2, Hints.empty)

  val values: List[Numbers] = List(
    ONE,
    TWO,
  )
  implicit val schema: Schema[Numbers] = intEnumeration(values).withId(id).addHints(hints)
}
