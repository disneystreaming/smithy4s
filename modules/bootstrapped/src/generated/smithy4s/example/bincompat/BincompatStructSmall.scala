package smithy4s.example.bincompat

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final class BincompatStructSmall private(val base1: String, val added2_1: Option[String]) extends Serializable {
  def withBase1(base1: String): BincompatStructSmall = copy(base1 = base1)
  def withAdded2_1(added2_1: Option[String]): BincompatStructSmall = copy(added2_1 = added2_1)

  private def copy(base1: String = this.base1, added2_1: Option[String] = this.added2_1): BincompatStructSmall = new BincompatStructSmall(base1, added2_1)
  override def equals(another: Any): Boolean = another match {
    case another: BincompatStructSmall => (this eq another) || this.base1 == another.base1 && this.added2_1 == another.added2_1
    case _ => false
  }
  override def hashCode(): Int = 37 * (37 * (37 * (17 + "smithy4s.example.bincompat.BincompatStructSmall".##) + this.base1.##) + this.added2_1.##)
  override def toString(): String = "BincompatStructSmall(" + this.base1 + ", " + this.added2_1 + ")"
}

object BincompatStructSmall extends ShapeTag.Companion[BincompatStructSmall] {
  val id: ShapeId = ShapeId("smithy4s.example.bincompat", "BincompatStructSmall")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(base1: String, added2_1: Option[String]): BincompatStructSmall = new BincompatStructSmall(base1, added2_1)

  // Members available since the beginning
  def apply(base1: String): BincompatStructSmall = new BincompatStructSmall(base1 = base1, added2_1 = None)
  // Members available up to version 2.0 (inclusive)
  def apply(base1: String, added2_1: Option[String]): BincompatStructSmall = new BincompatStructSmall(base1 = base1, added2_1 = added2_1)

  implicit val schema: Schema[BincompatStructSmall] = struct(
    string.required[BincompatStructSmall]("base1", _.base1),
    string.optional[BincompatStructSmall]("added2_1", _.added2_1),
  )(make).withId(id).addHints(hints)
}
