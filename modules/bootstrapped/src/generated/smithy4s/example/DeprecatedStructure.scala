package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

@deprecated(message = "A compelling reason", since = "0.0.1")
final case class DeprecatedStructure(@deprecated(message = "N/A", since = "N/A") strings: Option[List[String]] = None, other: Option[List[String]] = None, @deprecated(message = "N/A", since = "N/A") name: Option[String] = None, nameV2: Option[String] = None) extends DeprecatedMixin

object DeprecatedStructure extends ShapeTag.Companion[DeprecatedStructure] {
  val id: ShapeId = ShapeId("smithy4s.example", "DeprecatedStructure")

  val hints: Hints = Hints(
    smithy.api.Deprecated(message = Some("A compelling reason"), since = Some("0.0.1")),
  )

  implicit val schema: Schema[DeprecatedStructure] = struct(
    Strings.underlyingSchema.optional[DeprecatedStructure]("strings", _.strings).addHints(smithy.api.Deprecated(message = None, since = None)),
    Strings.underlyingSchema.optional[DeprecatedStructure]("other", _.other),
    string.optional[DeprecatedStructure]("name", _.name).addHints(smithy.api.Deprecated(message = None, since = None)),
    string.optional[DeprecatedStructure]("nameV2", _.nameV2),
  ){
    DeprecatedStructure.apply
  }.withId(id).addHints(hints)
}
