package smithy4s.benchmark

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class CreateObjectInput(key: String, bucketName: String, payload: S3Object)

object CreateObjectInput extends ShapeTag.Companion[CreateObjectInput] {
  val id: ShapeId = ShapeId("smithy4s.benchmark", "CreateObjectInput")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[CreateObjectInput] = struct(
    string.required[CreateObjectInput]("key", _.key).addHints(smithy.api.HttpLabel()),
    string.required[CreateObjectInput]("bucketName", _.bucketName).addHints(smithy.api.HttpLabel()),
    S3Object.schema.required[CreateObjectInput]("payload", _.payload).addHints(smithy.api.HttpPayload()),
  ){
    CreateObjectInput.apply
  }.withId(id).addHints(hints)
}
