package smithy4s.example.bincompat

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final class BincompatFriendlyTraitStruct private(val base1: String, val base2: String, val added2_1: String, val added3_1: String, val base3: Option[String]) extends Serializable {
  def withBase1(base1: String): BincompatFriendlyTraitStruct = copy(base1 = base1)
  def withBase2(base2: String): BincompatFriendlyTraitStruct = copy(base2 = base2)
  def withAdded2_1(added2_1: String): BincompatFriendlyTraitStruct = copy(added2_1 = added2_1)
  def withAdded3_1(added3_1: String): BincompatFriendlyTraitStruct = copy(added3_1 = added3_1)
  def withBase3(base3: Option[String]): BincompatFriendlyTraitStruct = copy(base3 = base3)

  private def copy(base1: String = this.base1, base2: String = this.base2, added2_1: String = this.added2_1, added3_1: String = this.added3_1, base3: Option[String] = this.base3): BincompatFriendlyTraitStruct = new BincompatFriendlyTraitStruct(base1, base2, added2_1, added3_1, base3)
  override def equals(another: Any): Boolean = another match {
    case another: BincompatFriendlyTraitStruct => (this eq another) || this.base1 == another.base1 && this.base2 == another.base2 && this.added2_1 == another.added2_1 && this.added3_1 == another.added3_1 && this.base3 == another.base3
    case _ => false
  }
  override def hashCode(): Int = 37 * (37 * (37 * (37 * (37 * (37 * (17 + "smithy4s.example.bincompat.BincompatFriendlyTraitStruct".##) + this.base1.##) + this.base2.##) + this.added2_1.##) + this.added3_1.##) + this.base3.##)
  override def toString(): String = "BincompatFriendlyTraitStruct(" + this.base1 + ", " + this.base2 + ", " + this.added2_1 + ", " + this.added3_1 + ", " + this.base3 + ")"
}

object BincompatFriendlyTraitStruct extends ShapeTag.Companion[BincompatFriendlyTraitStruct] {
  val id: ShapeId = ShapeId("smithy4s.example.bincompat", "BincompatFriendlyTraitStruct")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("smithy.api", "trait"), smithy4s.Document.obj()),
  )

  // constructor using the original order from the spec
  private def make(base1: String, base2: String, base3: Option[String], added2_1: String, added3_1: String): BincompatFriendlyTraitStruct = new BincompatFriendlyTraitStruct(base1, base2, added2_1, added3_1, base3)

  // Members available since the beginning
  def apply(base1: String, base2: String, base3: Option[String] = None): BincompatFriendlyTraitStruct = new BincompatFriendlyTraitStruct(base1 = base1, base2 = base2, base3 = base3, added2_1 = "woop2_1", added3_1 = "woop3_1")
  // Members available up to version 2.0 (inclusive)
  def apply(base1: String, base2: String, base3: Option[String], added2_1: String): BincompatFriendlyTraitStruct = new BincompatFriendlyTraitStruct(base1 = base1, base2 = base2, base3 = base3, added2_1 = added2_1, added3_1 = "woop3_1")
  // Members available up to version 3.0 (inclusive)
  def apply(base1: String, base2: String, base3: Option[String], added2_1: String, added3_1: String): BincompatFriendlyTraitStruct = new BincompatFriendlyTraitStruct(base1 = base1, base2 = base2, base3 = base3, added2_1 = added2_1, added3_1 = added3_1)

  implicit val schema: Schema[BincompatFriendlyTraitStruct] = recursive(struct(
    string.required[BincompatFriendlyTraitStruct]("base1", _.base1),
    string.required[BincompatFriendlyTraitStruct]("base2", _.base2),
    string.optional[BincompatFriendlyTraitStruct]("base3", _.base3),
    string.field[BincompatFriendlyTraitStruct]("added2_1", _.added2_1).addHints(Hints.dynamic(ShapeId("smithy.api", "default"), smithy4s.Document.fromString("woop2_1"))),
    string.required[BincompatFriendlyTraitStruct]("added3_1", _.added3_1).addHints(Hints.dynamic(ShapeId("smithy.api", "default"), smithy4s.Document.fromString("woop3_1"))),
  )(make).withId(id).addHints(hints))
}
