package smithy4s.example

import _root_.smithy4s.Enumeration
import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.EnumTag
import _root_.smithy4s.schema.Schema.enumeration

sealed abstract class UnknownServerErrorCode(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = UnknownServerErrorCode
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = UnknownServerErrorCode
  @inline final def widen: UnknownServerErrorCode = this
}
object UnknownServerErrorCode extends Enumeration[UnknownServerErrorCode] with ShapeTag.Companion[UnknownServerErrorCode] {
  val id: ShapeId = ShapeId("smithy4s.example", "UnknownServerErrorCode")

  val hints: Hints = Hints.empty

  case object ERROR_CODE extends UnknownServerErrorCode("server.error", "ERROR_CODE", 0, Hints())

  val values: List[UnknownServerErrorCode] = List(
    ERROR_CODE,
  )
  val tag: EnumTag[UnknownServerErrorCode] = EnumTag.ClosedStringEnum
  implicit val schema: Schema[UnknownServerErrorCode] = enumeration(tag, values).withId(id).addHints(hints)
}
