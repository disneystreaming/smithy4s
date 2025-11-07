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

  // constructor using the original order from the spec
  private def make(key: String, bucketName: String, body: String): SendStringInput = SendStringInput(key, bucketName, body)

  implicit val schema: Schema[SendStringInput] = struct(
    string.required[SendStringInput]("key", _.key).addHints(Hints.dynamic(ShapeId("smithy.api", "httpLabel"), smithy4s.Document.obj())),
    string.required[SendStringInput]("bucketName", _.bucketName).addHints(Hints.dynamic(ShapeId("smithy.api", "httpLabel"), smithy4s.Document.obj())),
    string.required[SendStringInput]("body", _.body).addHints(Hints.dynamic(ShapeId("smithy.api", "httpPayload"), smithy4s.Document.obj())),
  )(make).withId(id).addHints(hints)
}
