package smithy4s.example

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
    smithy.api.Documentation("A key and bucket is always required for putting a new file in a bucket"),
  ).lazily

  // constructor using the original order from the spec
  private def make(key: ObjectKey, bucketName: BucketName, foo: Option[LowHigh], someValue: Option[SomeValue], data: String): PutObjectInput = PutObjectInput(key, bucketName, data, foo, someValue)

  implicit val schema: Schema[PutObjectInput] = struct(
    ObjectKey.schema.required[PutObjectInput]("key", _.key).addHints(smithy.api.HttpLabel()),
    BucketName.schema.required[PutObjectInput]("bucketName", _.bucketName).addHints(smithy.api.HttpLabel()),
    LowHigh.schema.optional[PutObjectInput]("foo", _.foo).addHints(smithy.api.HttpHeader("X-Foo")),
    SomeValue.schema.optional[PutObjectInput]("someValue", _.someValue).addHints(smithy.api.HttpQuery("paramName")),
    string.required[PutObjectInput]("data", _.data).addHints(smithy.api.HttpPayload()),
  ){
    make
  }.withId(id).addHints(hints)
}
