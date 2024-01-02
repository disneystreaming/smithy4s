package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.stringEnumeration

sealed abstract class UnknownServerErrorCode(_name: String, _stringValue: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = UnknownServerErrorCode
  override val name: String = _name
  override val stringValue: String = _stringValue
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = UnknownServerErrorCode
  @inline final def widen: UnknownServerErrorCode = this
}
object UnknownServerErrorCode extends Enumeration[UnknownServerErrorCode] with ShapeTag.Companion[UnknownServerErrorCode] {
  val id: ShapeId = ShapeId("smithy4s.example", "UnknownServerErrorCode")

  val hints: Hints = Hints.empty

  case object ERROR_CODE extends UnknownServerErrorCode("ERROR_CODE", "server.error", 0, Hints())

  val values: List[UnknownServerErrorCode] = List(
    ERROR_CODE,
  )
  implicit val schema: Schema[UnknownServerErrorCode] = stringEnumeration(values).withId(id).addHints(hints)
}
