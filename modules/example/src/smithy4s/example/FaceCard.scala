package smithy4s.example

import smithy4s.schema.Schema._

sealed abstract class FaceCard(_value: String, _ordinal: Int) extends smithy4s.Enumeration.Value {
  override val value : String = _value
  override val ordinal: Int = _ordinal
  override val hints: smithy4s.Hints = smithy4s.Hints.empty
}
object FaceCard extends smithy4s.Enumeration[FaceCard] with smithy4s.ShapeTag.Companion[FaceCard] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "FaceCard")

  val hints : smithy4s.Hints = smithy4s.Hints(
    smithy4s.IntEnum(),
  )

  case object JACK extends FaceCard("1", 0)
  case object QUEEN extends FaceCard("2", 1)
  case object KING extends FaceCard("3", 2)
  case object ACE extends FaceCard("4", 3)
  case object JOKER extends FaceCard("5", 4)

  val values: List[FaceCard] = List(
    JACK,
    QUEEN,
    KING,
    ACE,
    JOKER,
  )
  implicit val schema: smithy4s.Schema[FaceCard] = enumeration(values).withId(id).addHints(hints)
}