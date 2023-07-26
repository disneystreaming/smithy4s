package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.interopcats.SchemaVisitorHash
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class MovieTheater(name: Option[String] = None)
object MovieTheater extends ShapeTag.$Companion[MovieTheater] {
  val $id: ShapeId = ShapeId("smithy4s.example", "MovieTheater")

  val $hints: Hints = Hints(
    smithy4s.example.Hash(),
  )

  val name: FieldLens[MovieTheater, Option[String]] = string.optional[MovieTheater]("name", _.name, n => c => c.copy(name = n))

  implicit val $schema: Schema[MovieTheater] = struct(
    name,
  ){
    MovieTheater.apply
  }.withId($id).addHints($hints)

  implicit val movieTheaterHash: cats.Hash[MovieTheater] = SchemaVisitorHash.fromSchema($schema)
}