package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class GetObjectOutput(size: ObjectSize, data: Option[String] = None)
object GetObjectOutput extends ShapeTag.Companion[GetObjectOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetObjectOutput")

  val hints: Hints = Hints.empty

  object Optics {
    val size = Lens[GetObjectOutput, ObjectSize](_.size)(n => a => a.copy(size = n))
    val data = Lens[GetObjectOutput, Option[String]](_.data)(n => a => a.copy(data = n))
  }

  implicit val schema: Schema[GetObjectOutput] = struct(
    ObjectSize.schema.required[GetObjectOutput]("size", _.size).addHints(smithy.api.HttpHeader("X-Size"), smithy.api.Required()),
    string.optional[GetObjectOutput]("data", _.data).addHints(smithy.api.HttpPayload()),
  ){
    GetObjectOutput.apply
  }.withId(id).addHints(hints)
}
