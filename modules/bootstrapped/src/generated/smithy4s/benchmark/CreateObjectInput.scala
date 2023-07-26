package smithy4s.benchmark

import smithy.api.HttpLabel
import smithy.api.HttpPayload
import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class CreateObjectInput(key: String, bucketName: String, payload: S3Object)
object CreateObjectInput extends ShapeTag.Companion[CreateObjectInput] {

  val key = string.required[CreateObjectInput]("key", _.key, n => c => c.copy(key = n)).addHints(HttpLabel(), Required())
  val bucketName = string.required[CreateObjectInput]("bucketName", _.bucketName, n => c => c.copy(bucketName = n)).addHints(HttpLabel(), Required())
  val payload = S3Object.schema.required[CreateObjectInput]("payload", _.payload, n => c => c.copy(payload = n)).addHints(HttpPayload(), Required())

  implicit val schema: Schema[CreateObjectInput] = struct(
    key,
    bucketName,
    payload,
  ){
    CreateObjectInput.apply
  }
  .withId(ShapeId("smithy4s.benchmark", "CreateObjectInput"))
  .addHints(
    Hints.empty
  )
}
