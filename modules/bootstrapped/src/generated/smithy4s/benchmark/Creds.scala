package smithy4s.benchmark

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class Creds(user: Option[String] = None, key: Option[String] = None)

object Creds extends ShapeTag.Companion[Creds] {
  val id: ShapeId = ShapeId("smithy4s.benchmark", "Creds")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[Creds] = struct(
    string.optional[Creds]("user", _.user),
    string.optional[Creds]("key", _.key),
  ){
    Creds.apply
  }.withId(id).addHints(hints)
}
