package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.struct

final case class BigStruct(a1: Int, a2: Int, a3: Int, a4: Int, a5: Int, a6: Int, a7: Int, a8: Int, a9: Int, a10: Int, a11: Int, a12: Int, a13: Int, a14: Int, a15: Int, a16: Int, a17: Int, a18: Int, a19: Int, a20: Int, a21: Int, a22: Int, a23: Int)
object BigStruct extends ShapeTag.Companion[BigStruct] {
  val hints: Hints = Hints.empty

  val a1 = int.required[BigStruct]("a1", _.a1).addHints(smithy.api.Required())
  val a2 = int.required[BigStruct]("a2", _.a2).addHints(smithy.api.Required())
  val a3 = int.required[BigStruct]("a3", _.a3).addHints(smithy.api.Required())
  val a4 = int.required[BigStruct]("a4", _.a4).addHints(smithy.api.Required())
  val a5 = int.required[BigStruct]("a5", _.a5).addHints(smithy.api.Required())
  val a6 = int.required[BigStruct]("a6", _.a6).addHints(smithy.api.Required())
  val a7 = int.required[BigStruct]("a7", _.a7).addHints(smithy.api.Required())
  val a8 = int.required[BigStruct]("a8", _.a8).addHints(smithy.api.Required())
  val a9 = int.required[BigStruct]("a9", _.a9).addHints(smithy.api.Required())
  val a10 = int.required[BigStruct]("a10", _.a10).addHints(smithy.api.Required())
  val a11 = int.required[BigStruct]("a11", _.a11).addHints(smithy.api.Required())
  val a12 = int.required[BigStruct]("a12", _.a12).addHints(smithy.api.Required())
  val a13 = int.required[BigStruct]("a13", _.a13).addHints(smithy.api.Required())
  val a14 = int.required[BigStruct]("a14", _.a14).addHints(smithy.api.Required())
  val a15 = int.required[BigStruct]("a15", _.a15).addHints(smithy.api.Required())
  val a16 = int.required[BigStruct]("a16", _.a16).addHints(smithy.api.Required())
  val a17 = int.required[BigStruct]("a17", _.a17).addHints(smithy.api.Required())
  val a18 = int.required[BigStruct]("a18", _.a18).addHints(smithy.api.Required())
  val a19 = int.required[BigStruct]("a19", _.a19).addHints(smithy.api.Required())
  val a20 = int.required[BigStruct]("a20", _.a20).addHints(smithy.api.Required())
  val a21 = int.required[BigStruct]("a21", _.a21).addHints(smithy.api.Required())
  val a22 = int.required[BigStruct]("a22", _.a22).addHints(smithy.api.Required())
  val a23 = int.required[BigStruct]("a23", _.a23).addHints(smithy.api.Required())

  implicit val schema: Schema[BigStruct] = struct.genericArity(
    a1,
    a2,
    a3,
    a4,
    a5,
    a6,
    a7,
    a8,
    a9,
    a10,
    a11,
    a12,
    a13,
    a14,
    a15,
    a16,
    a17,
    a18,
    a19,
    a20,
    a21,
    a22,
    a23,
  ){
    arr => new BigStruct(
      arr(0).asInstanceOf[Int],
      arr(1).asInstanceOf[Int],
      arr(2).asInstanceOf[Int],
      arr(3).asInstanceOf[Int],
      arr(4).asInstanceOf[Int],
      arr(5).asInstanceOf[Int],
      arr(6).asInstanceOf[Int],
      arr(7).asInstanceOf[Int],
      arr(8).asInstanceOf[Int],
      arr(9).asInstanceOf[Int],
      arr(10).asInstanceOf[Int],
      arr(11).asInstanceOf[Int],
      arr(12).asInstanceOf[Int],
      arr(13).asInstanceOf[Int],
      arr(14).asInstanceOf[Int],
      arr(15).asInstanceOf[Int],
      arr(16).asInstanceOf[Int],
      arr(17).asInstanceOf[Int],
      arr(18).asInstanceOf[Int],
      arr(19).asInstanceOf[Int],
      arr(20).asInstanceOf[Int],
      arr(21).asInstanceOf[Int],
      arr(22).asInstanceOf[Int],
    )
  }.withId(ShapeId("smithy4s.example", "BigStruct")).addHints(hints)
}
