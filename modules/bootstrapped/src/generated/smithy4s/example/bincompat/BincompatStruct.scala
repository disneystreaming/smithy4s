package smithy4s.example.bincompat

import smithy4s.Hints
import smithy4s.Nullable
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final class BincompatStruct private(val base1: String, val base2: String, val base4: String, val added2_1: String, val added3_1: String, val added4_1: String, val base3: Option[String], val added2_2: Option[String], val added3_2: Option[String], val added4_2: Option[Nullable[String]]) extends Serializable {
  def withBase1(base1: String): BincompatStruct = copy(base1 = base1)
  def withBase2(base2: String): BincompatStruct = copy(base2 = base2)
  def withBase4(base4: String): BincompatStruct = copy(base4 = base4)
  def withAdded2_1(added2_1: String): BincompatStruct = copy(added2_1 = added2_1)
  def withAdded3_1(added3_1: String): BincompatStruct = copy(added3_1 = added3_1)
  def withAdded4_1(added4_1: String): BincompatStruct = copy(added4_1 = added4_1)
  def withBase3(base3: Option[String]): BincompatStruct = copy(base3 = base3)
  def withAdded2_2(added2_2: Option[String]): BincompatStruct = copy(added2_2 = added2_2)
  def withAdded3_2(added3_2: Option[String]): BincompatStruct = copy(added3_2 = added3_2)
  def withAdded4_2(added4_2: Option[Nullable[String]]): BincompatStruct = copy(added4_2 = added4_2)

  private def copy(base1: String = this.base1, base2: String = this.base2, base4: String = this.base4, added2_1: String = this.added2_1, added3_1: String = this.added3_1, added4_1: String = this.added4_1, base3: Option[String] = this.base3, added2_2: Option[String] = this.added2_2, added3_2: Option[String] = this.added3_2, added4_2: Option[Nullable[String]] = this.added4_2): BincompatStruct = new BincompatStruct(base1, base2, base4, added2_1, added3_1, added4_1, base3, added2_2, added3_2, added4_2)
  override def equals(another: Any): Boolean = another match {
    case another: BincompatStruct => (this eq another) || this.base1 == another.base1 && this.base2 == another.base2 && this.base4 == another.base4 && this.added2_1 == another.added2_1 && this.added3_1 == another.added3_1 && this.added4_1 == another.added4_1 && this.base3 == another.base3 && this.added2_2 == another.added2_2 && this.added3_2 == another.added3_2 && this.added4_2 == another.added4_2
    case _ => false
  }
  override def hashCode(): Int = 37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (17 + "smithy4s.example.bincompat.BincompatStruct".##) + this.base1.##) + this.base2.##) + this.base4.##) + this.added2_1.##) + this.added3_1.##) + this.added4_1.##) + this.base3.##) + this.added2_2.##) + this.added3_2.##) + this.added4_2.##)
  override def toString(): String = "BincompatStruct(" + this.base1 + ", " + this.base2 + ", " + this.base4 + ", " + this.added2_1 + ", " + this.added3_1 + ", " + this.added4_1 + ", " + this.base3 + ", " + this.added2_2 + ", " + this.added3_2 + ", " + this.added4_2 + ")"
}

object BincompatStruct extends ShapeTag.Companion[BincompatStruct] {
  val id: ShapeId = ShapeId("smithy4s.example.bincompat", "BincompatStruct")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(base1: String, base2: String, base3: Option[String], added2_1: String, added2_2: Option[String], added3_1: String, added3_2: Option[String], added4_1: String, added4_2: Option[Nullable[String]], base4: String): BincompatStruct = new BincompatStruct(base1, base2, base4, added2_1, added3_1, added4_1, base3, added2_2, added3_2, added4_2)

  // Members available since the beginning
  def apply(base1: String, base2: String, base3: Option[String] = None, base4: String): BincompatStruct = new BincompatStruct(base1 = base1, base2 = base2, base3 = base3, base4 = base4, added2_1 = "woop2_1", added2_2 = None, added3_1 = "woop3_1", added3_2 = None, added4_1 = "woop4_1", added4_2 = None)
  // Members available up to version 2.0 (inclusive)
  def apply(base1: String, base2: String, base3: Option[String], base4: String, added2_1: String, added2_2: Option[String]): BincompatStruct = new BincompatStruct(base1 = base1, base2 = base2, base3 = base3, base4 = base4, added2_1 = added2_1, added2_2 = added2_2, added3_1 = "woop3_1", added3_2 = None, added4_1 = "woop4_1", added4_2 = None)
  // Members available up to version 3.0 (inclusive)
  def apply(base1: String, base2: String, base3: Option[String], base4: String, added2_1: String, added2_2: Option[String], added3_1: String, added3_2: Option[String]): BincompatStruct = new BincompatStruct(base1 = base1, base2 = base2, base3 = base3, base4 = base4, added2_1 = added2_1, added2_2 = added2_2, added3_1 = added3_1, added3_2 = added3_2, added4_1 = "woop4_1", added4_2 = None)
  // Members available up to version 4.0 (inclusive)
  def apply(base1: String, base2: String, base3: Option[String], base4: String, added2_1: String, added2_2: Option[String], added3_1: String, added3_2: Option[String], added4_1: String, added4_2: Option[Nullable[String]]): BincompatStruct = new BincompatStruct(base1 = base1, base2 = base2, base3 = base3, base4 = base4, added2_1 = added2_1, added2_2 = added2_2, added3_1 = added3_1, added3_2 = added3_2, added4_1 = added4_1, added4_2 = added4_2)

  implicit val schema: Schema[BincompatStruct] = struct(
    string.required[BincompatStruct]("base1", _.base1),
    string.required[BincompatStruct]("base2", _.base2),
    string.optional[BincompatStruct]("base3", _.base3),
    string.required[BincompatStruct]("added2_1", _.added2_1).addHints(Hints.dynamic(ShapeId("smithy.api", "default"), smithy4s.Document.fromString("woop2_1"))),
    string.optional[BincompatStruct]("added2_2", _.added2_2),
    string.required[BincompatStruct]("added3_1", _.added3_1).addHints(Hints.dynamic(ShapeId("smithy.api", "default"), smithy4s.Document.fromString("woop3_1"))),
    string.optional[BincompatStruct]("added3_2", _.added3_2),
    string.field[BincompatStruct]("added4_1", _.added4_1).addHints(Hints.dynamic(ShapeId("smithy.api", "default"), smithy4s.Document.fromString("woop4_1"))),
    string.nullable.optional[BincompatStruct]("added4_2", _.added4_2),
    string.required[BincompatStruct]("base4", _.base4),
  )(make).withId(id).addHints(hints)
}
