package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.list
import smithy4s.schema.Schema.map
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class InlineCollections(requiredStrings: List[String] = List(), strings: Option[List[String]] = None, structs: Option[List[InlineCollectionsItem]] = None, nested: Option[List[List[Int]]] = None, stringToInt: Option[Map[String, Int]] = None, stringToStructs: Option[Map[String, List[InlineCollectionsItem]]] = None, ref: Option[List[String]] = None)

object InlineCollections extends ShapeTag.Companion[InlineCollections] {
  val id: ShapeId = ShapeId("smithy4s.example", "InlineCollections")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(strings: Option[List[String]], requiredStrings: List[String], structs: Option[List[InlineCollectionsItem]], nested: Option[List[List[Int]]], stringToInt: Option[Map[String, Int]], stringToStructs: Option[Map[String, List[InlineCollectionsItem]]], ref: Option[List[String]]): InlineCollections = InlineCollections(requiredStrings, strings, structs, nested, stringToInt, stringToStructs, ref)

  implicit val schema: Schema[InlineCollections] = struct[InlineCollections](
    list(string).optional[InlineCollections]("strings", _.strings),
    list(string).required[InlineCollections]("requiredStrings", _.requiredStrings).addHints(Hints.dynamic(ShapeId("smithy.api", "default"), smithy4s.Document.array())),
    list(InlineCollectionsItem.schema).optional[InlineCollections]("structs", _.structs),
    list(list(int)).optional[InlineCollections]("nested", _.nested),
    map(string, int).optional[InlineCollections]("stringToInt", _.stringToInt),
    map(string, list(InlineCollectionsItem.schema)).optional[InlineCollections]("stringToStructs", _.stringToStructs),
    StringList.underlyingSchema.optional[InlineCollections]("ref", _.ref),
  )(make).withId(id).addHints(hints)
}
