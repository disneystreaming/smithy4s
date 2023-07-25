package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

@deprecated(message = "A compelling reason", since = "0.0.1")
final case class DeprecatedStructure(@deprecated(message = "N/A", since = "N/A") name: Option[String] = None, nameV2: Option[String] = None, strings: Option[List[String]] = None)
object DeprecatedStructure extends ShapeTag.Companion[DeprecatedStructure] {
  val hints: Hints = Hints(
    smithy.api.Deprecated(message = Some("A compelling reason"), since = Some("0.0.1")),
  )

  val name = string.optional[DeprecatedStructure]("name", _.name).addHints(smithy.api.Deprecated(message = None, since = None))
  val nameV2 = string.optional[DeprecatedStructure]("nameV2", _.nameV2)
  val strings = Strings.underlyingSchema.optional[DeprecatedStructure]("strings", _.strings)

  implicit val schema: Schema[DeprecatedStructure] = struct(
    name,
    nameV2,
    strings,
  ){
    DeprecatedStructure.apply
  }.withId(ShapeId("smithy4s.example", "DeprecatedStructure")).addHints(hints)
}
