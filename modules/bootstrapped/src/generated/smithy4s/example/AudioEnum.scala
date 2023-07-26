package smithy4s.example

import smithy.api.MediaType
import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.EnumTag
import smithy4s.schema.Schema.enumeration

sealed abstract class AudioEnum(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = AudioEnum
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = AudioEnum
  @inline final def widen: AudioEnum = this
}
object AudioEnum extends Enumeration[AudioEnum] with ShapeTag.Companion[AudioEnum] {
  case object GUITAR extends AudioEnum("guitar", "GUITAR", 0, Hints())
  case object BASS extends AudioEnum("bass", "BASS", 1, Hints())

  val values: List[AudioEnum] = List(
    GUITAR,
    BASS,
  )
  val tag: EnumTag = EnumTag.StringEnum
  implicit val schema: Schema[AudioEnum] = enumeration(tag, values)
  .withId(ShapeId("smithy4s.example", "AudioEnum"))
  .addHints(
    MediaType("audio/mpeg3"),
  )
}
