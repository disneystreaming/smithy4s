package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.struct

/** @param foo
  *   Helpful information for Foo
  *   int, bigInt and bDec are useful number constructs
  *   The string case is there because.
  */
final case class GetFooOutput(foo: Option[Foo] = None)
object GetFooOutput extends ShapeTag.$Companion[GetFooOutput] {
  val $id: ShapeId = ShapeId("smithy4s.example", "GetFooOutput")

  val $hints: Hints = Hints.empty

  val foo: FieldLens[GetFooOutput, Option[Foo]] = Foo.$schema.optional[GetFooOutput]("foo", _.foo, n => c => c.copy(foo = n))

  implicit val $schema: Schema[GetFooOutput] = struct(
    foo,
  ){
    GetFooOutput.apply
  }.withId($id).addHints($hints)
}