package smithy4s.benchmark

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class SendStringInput(key: String, bucketName: String, body: String)
object SendStringInput extends ShapeTag.Companion[SendStringInput] {
  val id: ShapeId = ShapeId("smithy4s.benchmark", "SendStringInput")

  val hints: Hints = Hints.empty

  object Optics {
    val key = Lens[SendStringInput, String](_.key)(n => a => a.copy(key = n))
    val bucketName = Lens[SendStringInput, String](_.bucketName)(n => a => a.copy(bucketName = n))
    val body = Lens[SendStringInput, String](_.body)(n => a => a.copy(body = n))
  }

  implicit val schema: Schema[SendStringInput] = struct(
    string.required[SendStringInput]("key", _.key).addHints(smithy.api.HttpLabel(), smithy.api.Required()),
    string.required[SendStringInput]("bucketName", _.bucketName).addHints(smithy.api.HttpLabel(), smithy.api.Required()),
    string.required[SendStringInput]("body", _.body).addHints(smithy.api.HttpPayload(), smithy.api.Required()),
  ){
    SendStringInput.apply
  }.withId(id).addHints(hints)
}
