package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.interopcats.SchemaVisitorHash
import smithy4s.schema.Schema.string

final case class MovieTheater(name: Option[String] = None)

object MovieTheater extends ShapeTag.Companion[MovieTheater] {
  val id: ShapeId = ShapeId("smithy4s.example", "MovieTheater")

  val hints: Hints = Hints(
    smithy4s.example.Hash(),
  )

  implicit val schema: Schema[MovieTheater] = struct(
    string.optional[MovieTheater]("name", _.name),
  ){
    MovieTheater.apply
  }.withId(id).addHints(hints)

  implicit val movieTheaterHash: cats.Hash[MovieTheater] = SchemaVisitorHash.fromSchema(schema)
}
