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

  // constructor using the original order from the spec
  private def make(key: String, bucketName: String, payload: S3Object): CreateObjectInput = CreateObjectInput(key, bucketName, payload)

  implicit val schema: Schema[CreateObjectInput] = struct(
    string.required[CreateObjectInput]("key", _.key).addHints(Hints.dynamic(ShapeId("smithy.api", "httpLabel"), smithy4s.Document.obj())),
    string.required[CreateObjectInput]("bucketName", _.bucketName).addHints(Hints.dynamic(ShapeId("smithy.api", "httpLabel"), smithy4s.Document.obj())),
    S3Object.schema.required[CreateObjectInput]("payload", _.payload).addHints(Hints.dynamic(ShapeId("smithy.api", "httpPayload"), smithy4s.Document.obj())),
  )(make).withId(id).addHints(hints)
}
