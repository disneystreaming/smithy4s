package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Prism
import smithy4s.schema.EnumTag
import smithy4s.schema.Schema.enumeration

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

  object Optics {
    val ERROR_CODE = Prism.partial[UnknownServerErrorCode, UnknownServerErrorCode.ERROR_CODE.type]{ case UnknownServerErrorCode.ERROR_CODE => UnknownServerErrorCode.ERROR_CODE }(identity)
  }

  case object ERROR_CODE extends UnknownServerErrorCode("server.error", "ERROR_CODE", 0, Hints())

  val values: List[UnknownServerErrorCode] = List(
    ERROR_CODE,
  )
  val tag: EnumTag = EnumTag.StringEnum
  implicit val schema: Schema[UnknownServerErrorCode] = enumeration(tag, values).withId(id).addHints(hints)
}
