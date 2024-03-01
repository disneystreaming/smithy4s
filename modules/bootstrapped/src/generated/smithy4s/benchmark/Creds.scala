package smithy4s.benchmark

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class Creds(user: Option[String] = None, key: Option[String] = None)

object Creds extends ShapeTag.Companion[Creds] {
  val id: ShapeId = ShapeId("smithy4s.benchmark", "Creds")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(user: Option[String], key: Option[String]): Creds = Creds(user, key)

  implicit val schema: Schema[Creds] = struct(
    string.optional[Creds]("user", _.user),
    string.optional[Creds]("key", _.key),
  ){
    make
  }.withId(id).addHints(hints)
}
