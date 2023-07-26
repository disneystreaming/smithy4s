package smithy4s.benchmark

import smithy.api.HttpLabel
import smithy.api.HttpPayload
import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class SendStringInput(key: String, bucketName: String, body: String)
object SendStringInput extends ShapeTag.$Companion[SendStringInput] {
  val $id: ShapeId = ShapeId("smithy4s.benchmark", "SendStringInput")

  val $hints: Hints = Hints.empty

  val key: FieldLens[SendStringInput, String] = string.required[SendStringInput]("key", _.key, n => c => c.copy(key = n)).addHints(HttpLabel(), Required())
  val bucketName: FieldLens[SendStringInput, String] = string.required[SendStringInput]("bucketName", _.bucketName, n => c => c.copy(bucketName = n)).addHints(HttpLabel(), Required())
  val body: FieldLens[SendStringInput, String] = string.required[SendStringInput]("body", _.body, n => c => c.copy(body = n)).addHints(HttpPayload(), Required())

  implicit val $schema: Schema[SendStringInput] = struct(
    key,
    bucketName,
    body,
  ){
    SendStringInput.apply
  }.withId($id).addHints($hints)
}
