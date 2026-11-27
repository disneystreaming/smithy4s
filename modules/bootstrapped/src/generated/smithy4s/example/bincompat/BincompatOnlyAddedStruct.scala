package smithy4s.example.bincompat

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final class BincompatOnlyAddedStruct private(val added2_1: Option[String]) extends Serializable {
  def withAdded2_1(added2_1: Option[String]): BincompatOnlyAddedStruct = copy(added2_1 = added2_1)

  private def copy(added2_1: Option[String]): BincompatOnlyAddedStruct = new BincompatOnlyAddedStruct(added2_1)
  override def equals(another: Any): Boolean = another match {
    case another: BincompatOnlyAddedStruct => (this eq another) || this.added2_1 == another.added2_1
    case _ => false
  }
  override def hashCode(): Int = 37 * (37 * (17 + "smithy4s.example.bincompat.BincompatOnlyAddedStruct".##) + this.added2_1.##)
  override def toString(): String = "BincompatOnlyAddedStruct(" + this.added2_1 + ")"
}

object BincompatOnlyAddedStruct extends ShapeTag.Companion[BincompatOnlyAddedStruct] {
  val id: ShapeId = ShapeId("smithy4s.example.bincompat", "BincompatOnlyAddedStruct")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(added2_1: Option[String]): BincompatOnlyAddedStruct = new BincompatOnlyAddedStruct(added2_1)

  // Members available since the beginning
  def apply(): BincompatOnlyAddedStruct = new BincompatOnlyAddedStruct(added2_1 = None)
  // Members available up to version 2.0 (inclusive)
  def apply(added2_1: Option[String]): BincompatOnlyAddedStruct = new BincompatOnlyAddedStruct(added2_1 = added2_1)

  implicit val schema: Schema[BincompatOnlyAddedStruct] = struct[BincompatOnlyAddedStruct](
    string.optional[BincompatOnlyAddedStruct]("added2_1", _.added2_1),
  )(make).withId(id).addHints(hints)
}
