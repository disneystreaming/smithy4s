package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.intEnumeration

sealed abstract class OneTwo(_name: String, _stringValue: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = OneTwo
  override val name: String = _name
  override val stringValue: String = _stringValue
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = OneTwo
  @inline final def widen: OneTwo = this
}
object OneTwo extends Enumeration[OneTwo] with ShapeTag.Companion[OneTwo] {
  val id: ShapeId = ShapeId("smithy4s.example", "oneTwo")

  val hints: Hints = Hints.empty

  case object ONE extends OneTwo("ONE", "ONE", 1, Hints())
  case object TWO extends OneTwo("TWO", "TWO", 2, Hints())

  val values: List[OneTwo] = List(
    ONE,
    TWO,
  )
  implicit val schema: Schema[OneTwo] = intEnumeration(values).withId(id).addHints(hints)
}
