package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.EnumTag
import smithy4s.schema.Schema.enumeration

sealed abstract class Numbers(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = Numbers
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = Numbers
  @inline final def widen: Numbers = this
}
object Numbers extends Enumeration[Numbers] with ShapeTag.Companion[Numbers] {
  val id: ShapeId = ShapeId("smithy4s.example", "Numbers")

  val hints: Hints = Hints(
    smithy.api.Box(),
  ).lazily

  case object ONE extends Numbers("ONE", "ONE", 1, Hints.empty)
  case object TWO extends Numbers("TWO", "TWO", 2, Hints.empty)

  val values: List[Numbers] = List(
    ONE,
    TWO,
  )
  val tag: EnumTag[Numbers] = EnumTag.ClosedIntEnum
  implicit val schema: Schema[Numbers] = enumeration(tag, values).withId(id).addHints(hints)
}
