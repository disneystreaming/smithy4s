package smithy4s.example.bincompat

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

final class BincompatEmptyStruct private() extends Serializable {

  override def equals(another: Any): Boolean = another.isInstanceOf[BincompatEmptyStruct]
  override def hashCode(): Int = 37 * (17 + "smithy4s.example.bincompat.BincompatEmptyStruct".##)
  override def toString(): String = "BincompatEmptyStruct()"
}

object BincompatEmptyStruct extends ShapeTag.Companion[BincompatEmptyStruct] {
  val id: ShapeId = ShapeId("smithy4s.example.bincompat", "BincompatEmptyStruct")

  val hints: Hints = Hints.empty


  // Members available since the beginning
  def apply(): BincompatEmptyStruct = new BincompatEmptyStruct()

  implicit val schema: Schema[BincompatEmptyStruct] = constant(BincompatEmptyStruct()).withId(id).addHints(hints)
}
