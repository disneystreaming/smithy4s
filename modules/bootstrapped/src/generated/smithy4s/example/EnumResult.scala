package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.EnumTag
import smithy4s.schema.Schema.enumeration

sealed abstract class EnumResult(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = EnumResult
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = EnumResult
  @inline final def widen: EnumResult = this
}
object EnumResult extends Enumeration[EnumResult] with ShapeTag.Companion[EnumResult] {
  val id: ShapeId = ShapeId("smithy4s.example", "EnumResult")

  val hints: Hints = Hints.empty

  case object FIRST extends EnumResult("FIRST", "FIRST", 1, Hints.empty)
  case object SECOND extends EnumResult("SECOND", "SECOND", 2, Hints.empty)

  val values: List[EnumResult] = List(
    FIRST,
    SECOND,
  )
  val tag: EnumTag[EnumResult] = EnumTag.ClosedIntEnum
  implicit val schema: Schema[EnumResult] = enumeration(tag, values).withId(id).addHints(hints)
}
