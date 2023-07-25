package smithy4s.benchmark

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class CreateObjectInput(key: String, bucketName: String, payload: S3Object)
object CreateObjectInput extends ShapeTag.Companion[CreateObjectInput] {
  val id: ShapeId = ShapeId("smithy4s.benchmark", "CreateObjectInput")

  val hints: Hints = Hints.empty

  val key = string.required[CreateObjectInput]("key", _.key).addHints(smithy.api.HttpLabel(), smithy.api.Required())
  val bucketName = string.required[CreateObjectInput]("bucketName", _.bucketName).addHints(smithy.api.HttpLabel(), smithy.api.Required())
  val payload = S3Object.schema.required[CreateObjectInput]("payload", _.payload).addHints(smithy.api.HttpPayload(), smithy.api.Required())

  implicit val schema: Schema[CreateObjectInput] = struct(
    key,
    bucketName,
    payload,
  ){
    CreateObjectInput.apply
  }.withId(id).addHints(hints)
}
