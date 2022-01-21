package smithy4s.example

import smithy4s.syntax._

sealed abstract class LowHigh(val value: String, val ordinal: Int) extends scala.Product with scala.Serializable
object LowHigh extends smithy4s.Enumeration[LowHigh] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "LowHigh")

  val hints : smithy4s.Hints = smithy4s.Hints(
    id,
  )

  case object Low extends LowHigh("Low", 0)
  case object High extends LowHigh("High", 1)

  val values: List[LowHigh] = List(
    Low,
    High,
  )

  def to(e: LowHigh) : (String, Int) = (e.value, e.ordinal)
  val schema: smithy4s.Schema[LowHigh] = enumeration(to, valueMap, ordinalMap).withHints(hints)
  implicit val staticSchema : schematic.Static[smithy4s.Schema[LowHigh]] = schematic.Static(schema)
}