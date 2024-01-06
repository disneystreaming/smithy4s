package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class HealthResponse(status: String)

object HealthResponse extends ShapeTag.Companion[HealthResponse] {
  val id: ShapeId = ShapeId("smithy4s.example", "HealthResponse")

  val hints: Hints = Hints(
    smithy4s.example.FreeForm(_root_.smithy4s.Document.obj("i" -> _root_.smithy4s.Document.fromDouble(1.0d), "a" -> _root_.smithy4s.Document.fromDouble(2.0d))),
  )

  implicit val schema: Schema[HealthResponse] = struct(
    string.required[HealthResponse]("status", _.status),
  ){
    HealthResponse.apply
  }.withId(id).addHints(hints)
}
