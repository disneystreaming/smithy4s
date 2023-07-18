package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.struct

final case class BigStruct(a1: Int, a2: Int, a3: Int, a4: Int, a5: Int, a6: Int, a7: Int, a8: Int, a9: Int, a10: Int, a11: Int, a12: Int, a13: Int, a14: Int, a15: Int, a16: Int, a17: Int, a18: Int, a19: Int, a20: Int, a21: Int, a22: Int, a23: Int)
object BigStruct extends ShapeTag.Companion[BigStruct] {
  val id: ShapeId = ShapeId("smithy4s.example", "BigStruct")

  val hints: Hints = Hints.empty

  object Lenses {
    val a1 = Lens[BigStruct, Int](_.a1)(n => a => a.copy(a1 = n))
    val a2 = Lens[BigStruct, Int](_.a2)(n => a => a.copy(a2 = n))
    val a3 = Lens[BigStruct, Int](_.a3)(n => a => a.copy(a3 = n))
    val a4 = Lens[BigStruct, Int](_.a4)(n => a => a.copy(a4 = n))
    val a5 = Lens[BigStruct, Int](_.a5)(n => a => a.copy(a5 = n))
    val a6 = Lens[BigStruct, Int](_.a6)(n => a => a.copy(a6 = n))
    val a7 = Lens[BigStruct, Int](_.a7)(n => a => a.copy(a7 = n))
    val a8 = Lens[BigStruct, Int](_.a8)(n => a => a.copy(a8 = n))
    val a9 = Lens[BigStruct, Int](_.a9)(n => a => a.copy(a9 = n))
    val a10 = Lens[BigStruct, Int](_.a10)(n => a => a.copy(a10 = n))
    val a11 = Lens[BigStruct, Int](_.a11)(n => a => a.copy(a11 = n))
    val a12 = Lens[BigStruct, Int](_.a12)(n => a => a.copy(a12 = n))
    val a13 = Lens[BigStruct, Int](_.a13)(n => a => a.copy(a13 = n))
    val a14 = Lens[BigStruct, Int](_.a14)(n => a => a.copy(a14 = n))
    val a15 = Lens[BigStruct, Int](_.a15)(n => a => a.copy(a15 = n))
    val a16 = Lens[BigStruct, Int](_.a16)(n => a => a.copy(a16 = n))
    val a17 = Lens[BigStruct, Int](_.a17)(n => a => a.copy(a17 = n))
    val a18 = Lens[BigStruct, Int](_.a18)(n => a => a.copy(a18 = n))
    val a19 = Lens[BigStruct, Int](_.a19)(n => a => a.copy(a19 = n))
    val a20 = Lens[BigStruct, Int](_.a20)(n => a => a.copy(a20 = n))
    val a21 = Lens[BigStruct, Int](_.a21)(n => a => a.copy(a21 = n))
    val a22 = Lens[BigStruct, Int](_.a22)(n => a => a.copy(a22 = n))
    val a23 = Lens[BigStruct, Int](_.a23)(n => a => a.copy(a23 = n))
  }

  implicit val schema: Schema[BigStruct] = struct.genericArity(
    int.required[BigStruct]("a1", _.a1).addHints(smithy.api.Required()),
    int.required[BigStruct]("a2", _.a2).addHints(smithy.api.Required()),
    int.required[BigStruct]("a3", _.a3).addHints(smithy.api.Required()),
    int.required[BigStruct]("a4", _.a4).addHints(smithy.api.Required()),
    int.required[BigStruct]("a5", _.a5).addHints(smithy.api.Required()),
    int.required[BigStruct]("a6", _.a6).addHints(smithy.api.Required()),
    int.required[BigStruct]("a7", _.a7).addHints(smithy.api.Required()),
    int.required[BigStruct]("a8", _.a8).addHints(smithy.api.Required()),
    int.required[BigStruct]("a9", _.a9).addHints(smithy.api.Required()),
    int.required[BigStruct]("a10", _.a10).addHints(smithy.api.Required()),
    int.required[BigStruct]("a11", _.a11).addHints(smithy.api.Required()),
    int.required[BigStruct]("a12", _.a12).addHints(smithy.api.Required()),
    int.required[BigStruct]("a13", _.a13).addHints(smithy.api.Required()),
    int.required[BigStruct]("a14", _.a14).addHints(smithy.api.Required()),
    int.required[BigStruct]("a15", _.a15).addHints(smithy.api.Required()),
    int.required[BigStruct]("a16", _.a16).addHints(smithy.api.Required()),
    int.required[BigStruct]("a17", _.a17).addHints(smithy.api.Required()),
    int.required[BigStruct]("a18", _.a18).addHints(smithy.api.Required()),
    int.required[BigStruct]("a19", _.a19).addHints(smithy.api.Required()),
    int.required[BigStruct]("a20", _.a20).addHints(smithy.api.Required()),
    int.required[BigStruct]("a21", _.a21).addHints(smithy.api.Required()),
    int.required[BigStruct]("a22", _.a22).addHints(smithy.api.Required()),
    int.required[BigStruct]("a23", _.a23).addHints(smithy.api.Required()),
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
  }.withId(id).addHints(hints)
}
