package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class DefaultNotCapitalized(name: Username = smithy4s.example.Username("hello"))

object DefaultNotCapitalized extends ShapeTag.Companion[DefaultNotCapitalized] {
  val id: ShapeId = ShapeId("smithy4s.example", "DefaultNotCapitalized")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(name: Username): DefaultNotCapitalized = DefaultNotCapitalized(name)

  implicit val schema: Schema[DefaultNotCapitalized] = struct(
    Username.schema.required[DefaultNotCapitalized]("name", _.name).addHints(smithy.api.Default(smithy4s.Document.fromString("hello"))),
  )(make).withId(id).addHints(hints)
}
