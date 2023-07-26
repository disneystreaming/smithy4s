package smithy4s.example

import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.interopcats.SchemaVisitorHash
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object PersonEmail extends Newtype[String] {
  val underlyingSchema: Schema[String] = string
  .withId(ShapeId("smithy4s.example", "PersonEmail"))
  .addHints(
    smithy4s.example.Hash(),
  )

  implicit val schema: Schema[PersonEmail] = bijection(underlyingSchema, asBijection)

  implicit val personEmailHash: cats.Hash[PersonEmail] = SchemaVisitorHash.fromSchema(schema)
}
