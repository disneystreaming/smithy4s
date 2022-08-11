package smithy4s.example

import smithy4s._
import smithy4s.schema.Schema._

case class ArbitraryDataTest()
object ArbitraryDataTest extends ShapeTag.Companion[ArbitraryDataTest] {
  val id: ShapeId = ShapeId("smithy4s.example", "ArbitraryDataTest")
  
  val hints : Hints = Hints(
    smithy4s.example.ArbitraryData(smithy4s.Document.obj("str" -> smithy4s.Document.fromString("hello"),"int" -> smithy4s.Document.fromDouble(1.0),"bool" -> smithy4s.Document.fromBoolean(true),"arr" -> smithy4s.Document.array(smithy4s.Document.fromString("one"),smithy4s.Document.fromString("two"),smithy4s.Document.fromString("three")),"obj" -> smithy4s.Document.obj("str" -> smithy4s.Document.fromString("s"),"i" -> smithy4s.Document.fromDouble(1.0)))),
  )
  
  implicit val schema: Schema[ArbitraryDataTest] = constant(ArbitraryDataTest()).withId(id).addHints(hints)
}