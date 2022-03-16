package smithy4s.example

import smithy4s.schema.Schema._

case class PutObjectInput(key: ObjectKey, bucketName: BucketName, data: String, foo: Option[LowHigh] = None, someValue: Option[SomeValue] = None)
object PutObjectInput extends smithy4s.ShapeTag.Companion[PutObjectInput] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "PutObjectInput")

  val hints : smithy4s.Hints = smithy4s.Hints.empty

  implicit val schema: smithy4s.Schema[PutObjectInput] = struct(
    ObjectKey.schema.required[PutObjectInput]("key", _.key).addHints(smithy.api.Required(), smithy.api.HttpLabel()),
    BucketName.schema.required[PutObjectInput]("bucketName", _.bucketName).addHints(smithy.api.Required(), smithy.api.HttpLabel()),
    string.required[PutObjectInput]("data", _.data).addHints(smithy.api.Required(), smithy.api.HttpPayload()),
    LowHigh.schema.optional[PutObjectInput]("foo", _.foo).addHints(smithy.api.HttpHeader("X-Foo")),
    SomeValue.schema.optional[PutObjectInput]("someValue", _.someValue).addHints(smithy.api.HttpQuery("paramName")),
  ){
    PutObjectInput.apply
  }.withId(id).addHints(hints)
}
