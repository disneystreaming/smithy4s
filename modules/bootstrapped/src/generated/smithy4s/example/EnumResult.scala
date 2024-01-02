package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.intEnumeration

sealed abstract class EnumResult(_name: String, _stringValue: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = EnumResult
  override val name: String = _name
  override val stringValue: String = _stringValue
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = EnumResult
  @inline final def widen: EnumResult = this
}
object EnumResult extends Enumeration[EnumResult] with ShapeTag.Companion[EnumResult] {
  val id: ShapeId = ShapeId("smithy4s.example", "EnumResult")

  val hints: Hints = Hints.empty

  case object FIRST extends EnumResult("FIRST", "FIRST", 1, Hints())
  case object SECOND extends EnumResult("SECOND", "SECOND", 2, Hints())

  val values: List[EnumResult] = List(
    FIRST,
    SECOND,
  )
  implicit val schema: Schema[EnumResult] = intEnumeration(values).withId(id).addHints(hints)
}
