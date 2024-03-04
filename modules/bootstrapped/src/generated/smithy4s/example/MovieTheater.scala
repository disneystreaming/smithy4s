package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.interopcats.SchemaVisitorHash
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class MovieTheater(name: Option[String] = None)

object MovieTheater extends ShapeTag.Companion[MovieTheater] {
  val id: ShapeId = ShapeId("smithy4s.example", "MovieTheater")

  val hints: Hints = Hints(
    smithy4s.example.Hash(),
  ).lazily

  // constructor using the original order from the spec
  private def make(name: Option[String]): MovieTheater = MovieTheater(name)

  implicit val schema: Schema[MovieTheater] = struct(
    string.optional[MovieTheater]("name", _.name),
  )(make).withId(id).addHints(hints)

  implicit val movieTheaterHash: cats.Hash[MovieTheater] = SchemaVisitorHash.fromSchema(schema)
}
