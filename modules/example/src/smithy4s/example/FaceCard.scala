package smithy4s.example

import smithy4s.schema.Schema._

sealed abstract class FaceCard(_value: String, _name: String, _ordinal: Int) extends smithy4s.Enumeration.Value {
  override val value: String = _value
  override val name: String = _name
  override val ordinal: Int = _ordinal
  override val hints: smithy4s.Hints = smithy4s.Hints.empty
  @inline final def widen: FaceCard = this
}
object FaceCard extends smithy4s.Enumeration[FaceCard] with smithy4s.ShapeTag.Companion[FaceCard] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "FaceCard")

  val hints : smithy4s.Hints = smithy4s.Hints(
    smithy.api.Documentation("FaceCard types"),
    smithy4s.IntEnum(),
  )

  case object JACK extends FaceCard("JACK", "JACK", 1)
  case object QUEEN extends FaceCard("QUEEN", "QUEEN", 2)
  case object KING extends FaceCard("KING", "KING", 3)
  case object ACE extends FaceCard("ACE", "ACE", 4)
  case object JOKER extends FaceCard("JOKER", "JOKER", 5)

  val values: List[FaceCard] = List(
    JACK,
    QUEEN,
    KING,
    ACE,
    JOKER,
  )
  implicit val schema: smithy4s.Schema[FaceCard] = enumeration(values).withId(id).addHints(hints)
}