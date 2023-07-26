package smithy4s.example

import smithy.api.Deprecated
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

@deprecated(message = "A compelling reason", since = "0.0.1")
final case class DeprecatedStructure(@deprecated(message = "N/A", since = "N/A") name: Option[String] = None, nameV2: Option[String] = None, strings: Option[List[String]] = None)
object DeprecatedStructure extends ShapeTag.Companion[DeprecatedStructure] {

  val name: FieldLens[DeprecatedStructure, Option[String]] = string.optional[DeprecatedStructure]("name", _.name, n => c => c.copy(name = n)).addHints(Deprecated(message = None, since = None))
  val nameV2: FieldLens[DeprecatedStructure, Option[String]] = string.optional[DeprecatedStructure]("nameV2", _.nameV2, n => c => c.copy(nameV2 = n))
  val strings: FieldLens[DeprecatedStructure, Option[List[String]]] = Strings.underlyingSchema.optional[DeprecatedStructure]("strings", _.strings, n => c => c.copy(strings = n))

  implicit val schema: Schema[DeprecatedStructure] = struct(
    name,
    nameV2,
    strings,
  ){
    DeprecatedStructure.apply
  }
  .withId(ShapeId("smithy4s.example", "DeprecatedStructure"))
  .addHints(
    Deprecated(message = Some("A compelling reason"), since = Some("0.0.1")),
  )
}
