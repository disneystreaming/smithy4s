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

  val key = string.required[SendStringInput]("key", _.key).addHints(smithy.api.HttpLabel(), smithy.api.Required())
  val bucketName = string.required[SendStringInput]("bucketName", _.bucketName).addHints(smithy.api.HttpLabel(), smithy.api.Required())
  val body = string.required[SendStringInput]("body", _.body).addHints(smithy.api.HttpPayload(), smithy.api.Required())

  implicit val schema: Schema[SendStringInput] = struct(
    key,
    bucketName,
    body,
  ){
    SendStringInput.apply
  }.withId(id).addHints(hints)
}
