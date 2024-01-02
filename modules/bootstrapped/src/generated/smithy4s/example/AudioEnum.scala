package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.stringEnumeration

sealed abstract class AudioEnum(_name: String, _stringValue: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = AudioEnum
  override val name: String = _name
  override val stringValue: String = _stringValue
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

  case object GUITAR extends AudioEnum("GUITAR", "guitar", 0, Hints())
  case object BASS extends AudioEnum("BASS", "bass", 1, Hints())

  val values: List[AudioEnum] = List(
    GUITAR,
    BASS,
  )
  implicit val schema: Schema[AudioEnum] = stringEnumeration(values).withId(id).addHints(hints)
}
