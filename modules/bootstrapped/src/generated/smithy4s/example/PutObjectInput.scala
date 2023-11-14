package smithy4s.example

import smithy4s.Document
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

/** A key and bucket is always required for putting a new file in a bucket */
final case class PutObjectInput(key: ObjectKey, bucketName: BucketName, data: String, foo: Option[LowHigh] = None, someValue: Option[SomeValue] = None)

object PutObjectInput extends ShapeTag.Companion[PutObjectInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "PutObjectInput")

  val hints: Hints = Hints(
    ShapeId("smithy.api", "documentation") -> Document.fromString("A key and bucket is always required for putting a new file in a bucket"),
  )

  implicit val schema: Schema[PutObjectInput] = struct(
    ObjectKey.schema.required[PutObjectInput]("key", _.key).addHints(smithy.api.HttpLabel()),
    BucketName.schema.required[PutObjectInput]("bucketName", _.bucketName).addHints(smithy.api.HttpLabel()),
    string.required[PutObjectInput]("data", _.data).addHints(smithy.api.HttpPayload()),
    LowHigh.schema.optional[PutObjectInput]("foo", _.foo).addHints(smithy.api.HttpHeader("X-Foo")),
    SomeValue.schema.optional[PutObjectInput]("someValue", _.someValue).addHints(smithy.api.HttpQuery("paramName")),
  ){
    PutObjectInput.apply
  }.withId(id).addHints(hints)
}
