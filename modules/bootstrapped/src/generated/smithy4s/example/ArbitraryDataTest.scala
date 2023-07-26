package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

final case class ArbitraryDataTest()
object ArbitraryDataTest extends ShapeTag.$Companion[ArbitraryDataTest] {
  val $id: ShapeId = ShapeId("smithy4s.example", "ArbitraryDataTest")

  val $hints: Hints = Hints(
    ArbitraryData(smithy4s.Document.obj("str" -> smithy4s.Document.fromString("hello"), "int" -> smithy4s.Document.fromDouble(1.0d), "bool" -> smithy4s.Document.fromBoolean(true), "arr" -> smithy4s.Document.array(smithy4s.Document.fromString("one"), smithy4s.Document.fromString("two"), smithy4s.Document.fromString("three")), "obj" -> smithy4s.Document.obj("str" -> smithy4s.Document.fromString("s"), "i" -> smithy4s.Document.fromDouble(1.0d)))),
  )

  implicit val $schema: Schema[ArbitraryDataTest] = constant(ArbitraryDataTest()).withId($id).addHints($hints)
}
