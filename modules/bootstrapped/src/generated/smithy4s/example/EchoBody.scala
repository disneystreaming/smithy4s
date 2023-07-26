package smithy4s.example

import smithy.api.Length
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class EchoBody(data: Option[String] = None)
object EchoBody extends ShapeTag.Companion[EchoBody] {

  val data: FieldLens[EchoBody, Option[String]] = string.validated(Length(min = Some(10L), max = None)).optional[EchoBody]("data", _.data, n => c => c.copy(data = n)).addHints()

  implicit val schema: Schema[EchoBody] = struct(
    data,
  ){
    EchoBody.apply
  }
  .withId(ShapeId("smithy4s.example", "EchoBody"))
}
