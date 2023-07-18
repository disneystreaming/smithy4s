package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.struct

/** @param foo
  *   Helpful information for Foo
  *   int, bigInt and bDec are useful number constructs
  *   The string case is there because.
  */
final case class GetFooOutput(foo: Option[Foo] = None)
object GetFooOutput extends ShapeTag.Companion[GetFooOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetFooOutput")

  val hints: Hints = Hints.empty

  object Lenses {
    val foo = Lens[GetFooOutput, Option[Foo]](_.foo)(n => a => a.copy(foo = n))
  }

  implicit val schema: Schema[GetFooOutput] = struct(
    Foo.schema.optional[GetFooOutput]("foo", _.foo),
  ){
    GetFooOutput.apply
  }.withId(id).addHints(hints)
}
