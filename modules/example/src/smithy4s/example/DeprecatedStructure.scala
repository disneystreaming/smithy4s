package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

@deprecated(message = "A compelling reason", since = "0.0.1")
case class DeprecatedStructure(@deprecated name: Option[String] = None, nameV2: Option[String] = None, strings: Option[List[String]] = None)
object DeprecatedStructure extends ShapeTag.Companion[DeprecatedStructure] {
  val id: ShapeId = ShapeId("smithy4s.example", "DeprecatedStructure")

  val hints: Hints = Hints(
    smithy.api.Deprecated(message = Some("A compelling reason"), since = Some("0.0.1")),
  )

  implicit val schema: Schema[DeprecatedStructure] = struct(
    string.optional[DeprecatedStructure]("name", _.name).addHints(smithy.api.Deprecated(message = None, since = None)),
    string.optional[DeprecatedStructure]("nameV2", _.nameV2),
    Strings.underlyingSchema.optional[DeprecatedStructure]("strings", _.strings),
  ){
    DeprecatedStructure.apply
  }.withId(id).addHints(hints)
}