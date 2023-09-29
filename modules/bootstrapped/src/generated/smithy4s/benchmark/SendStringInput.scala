package smithy4s.benchmark

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class SendStringInput(key: String, bucketName: String, body: String)

object SendStringInput extends ShapeTag.Companion[SendStringInput] {
  val id: ShapeId = ShapeId("smithy4s.benchmark", "SendStringInput")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[SendStringInput] = struct(
    string.required[SendStringInput]("key", _.key).addHints(smithy.api.HttpLabel()),
    string.required[SendStringInput]("bucketName", _.bucketName).addHints(smithy.api.HttpLabel()),
    string.required[SendStringInput]("body", _.body).addHints(smithy.api.HttpPayload()),
  ){
    SendStringInput.apply
  }.withId(id).addHints(hints)
}
