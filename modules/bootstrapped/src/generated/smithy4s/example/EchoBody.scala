package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class EchoBody(data: Option[String] = None)
object EchoBody extends ShapeTag.Companion[EchoBody] {
  val id: ShapeId = ShapeId("smithy4s.example", "EchoBody")

  val hints: Hints = Hints.empty

  object Optics {
    val dataLens = Lens[EchoBody, Option[String]](_.data)(n => a => a.copy(data = n))
  }

  implicit val schema: Schema[EchoBody] = struct(
    string.validated(smithy.api.Length(min = Some(10L), max = None)).optional[EchoBody]("data", _.data),
  ){
    EchoBody.apply
  }.withId(id).addHints(hints)
}
