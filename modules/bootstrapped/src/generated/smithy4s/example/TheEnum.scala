package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.stringEnumeration

sealed abstract class TheEnum(_name: String, _stringValue: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = TheEnum
  override val name: String = _name
  override val stringValue: String = _stringValue
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = TheEnum
  @inline final def widen: TheEnum = this
}
object TheEnum extends Enumeration[TheEnum] with ShapeTag.Companion[TheEnum] {
  val id: ShapeId = ShapeId("smithy4s.example", "TheEnum")

  val hints: Hints = Hints.empty

  case object V1 extends TheEnum("V1", "v1", 0, Hints())
  case object V2 extends TheEnum("V2", "v2", 1, Hints())

  val values: List[TheEnum] = List(
    V1,
    V2,
  )
  implicit val schema: Schema[TheEnum] = stringEnumeration(values).withId(id).addHints(hints)
}
