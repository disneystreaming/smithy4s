package smithy4s.example

import _root_.smithy4s.Enumeration
import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.EnumTag
import _root_.smithy4s.schema.Schema.enumeration

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
  val id: ShapeId = ShapeId("smithy4s.example", "AudioEnum")

  val hints: Hints = Hints(
    smithy.api.MediaType("audio/mpeg3"),
  )

  case object GUITAR extends AudioEnum("guitar", "GUITAR", 0, Hints())
  case object BASS extends AudioEnum("bass", "BASS", 1, Hints())

  val values: List[AudioEnum] = List(
    GUITAR,
    BASS,
  )
  val tag: EnumTag[AudioEnum] = EnumTag.ClosedStringEnum
  implicit val schema: Schema[AudioEnum] = enumeration(tag, values).withId(id).addHints(hints)
}
