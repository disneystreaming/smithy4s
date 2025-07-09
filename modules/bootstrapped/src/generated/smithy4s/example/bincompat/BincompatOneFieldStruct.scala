package smithy4s.example.bincompat

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final class BincompatOneFieldStruct private(val s: Option[String]) extends Serializable {
  def withS(s: Option[String]): BincompatOneFieldStruct = copy(s = s)

  private def copy(s: Option[String]): BincompatOneFieldStruct = new BincompatOneFieldStruct(s)
  override def equals(another: Any): Boolean = another match {
    case another: BincompatOneFieldStruct => (this eq another) || this.s == another.s
    case _ => false
  }
  override def hashCode(): Int = 37 * (37 * (17 + "smithy4s.example.bincompat.BincompatOneFieldStruct".##) + this.s.##)
  override def toString(): String = "BincompatOneFieldStruct(" + this.s + ")"
}

object BincompatOneFieldStruct extends ShapeTag.Companion[BincompatOneFieldStruct] {
  val id: ShapeId = ShapeId("smithy4s.example.bincompat", "BincompatOneFieldStruct")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(s: Option[String]): BincompatOneFieldStruct = new BincompatOneFieldStruct(s)

  // Members available since the beginning
  def apply(s: Option[String] = None): BincompatOneFieldStruct = new BincompatOneFieldStruct(s = s)

  implicit val schema: Schema[BincompatOneFieldStruct] = struct(
    string.optional[BincompatOneFieldStruct]("s", _.s),
  )(make).withId(id).addHints(hints)
}
