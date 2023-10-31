package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Smithy4sThrowable
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

case class ClientError(code: Int, details: String) extends Smithy4sThrowable {
}

object ClientError extends ShapeTag.Companion[ClientError] {
  val id: ShapeId = ShapeId("smithy4s.example", "ClientError")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
  )

  implicit val schema: Schema[ClientError] = struct(
    int.required[ClientError]("code", _.code).addHints(smithy.api.Required()),
    string.required[ClientError]("details", _.details).addHints(smithy.api.Required()),
  ){
    ClientError.apply
  }.withId(id).addHints(hints)
}