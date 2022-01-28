package smithy4s.example

import smithy4s.syntax._

case class ArbitraryDataTest()
object ArbitraryDataTest extends smithy4s.ShapeTag.Companion[ArbitraryDataTest] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "ArbitraryDataTest")

  val hints : smithy4s.Hints = smithy4s.Hints(
    id,
    smithy4s.example.ArbitraryData(smithy4s.Document.obj("str" -> smithy4s.Document.fromString("hello"),"int" -> smithy4s.Document.fromDouble(1.0),"bool" -> smithy4s.Document.fromBoolean(true),"arr" -> smithy4s.Document.array(smithy4s.Document.fromString("one"),smithy4s.Document.fromString("two"),smithy4s.Document.fromString("three")),"obj" -> smithy4s.Document.obj("str" -> smithy4s.Document.fromString("s"),"i" -> smithy4s.Document.fromDouble(1.0)))),
  )

  val schema: smithy4s.Schema[ArbitraryDataTest] = constant(ArbitraryDataTest()).withHints(hints)
  implicit val staticSchema : schematic.Static[smithy4s.Schema[ArbitraryDataTest]] = schematic.Static(schema)
}