package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.constant

final case class ArbitraryDataTest()

object ArbitraryDataTest extends ShapeTag.Companion[ArbitraryDataTest] {
  val id: ShapeId = ShapeId("smithy4s.example", "ArbitraryDataTest")

  val hints: Hints = Hints(
    smithy4s.example.ArbitraryData(_root_.smithy4s.Document.obj("str" -> _root_.smithy4s.Document.fromString("hello"), "int" -> _root_.smithy4s.Document.fromDouble(1.0d), "bool" -> _root_.smithy4s.Document.fromBoolean(true), "arr" -> _root_.smithy4s.Document.array(_root_.smithy4s.Document.fromString("one"), _root_.smithy4s.Document.fromString("two"), _root_.smithy4s.Document.fromString("three")), "obj" -> _root_.smithy4s.Document.obj("str" -> _root_.smithy4s.Document.fromString("s"), "i" -> _root_.smithy4s.Document.fromDouble(1.0d)))),
  )

  implicit val schema: Schema[ArbitraryDataTest] = constant(ArbitraryDataTest()).withId(id).addHints(hints)
}
