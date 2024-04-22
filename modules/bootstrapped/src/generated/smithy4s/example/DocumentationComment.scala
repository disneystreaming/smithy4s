package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

/** We should be able to use comments in documentation /&ast; \*\/
  * @param member
  *   /&ast;
  */
final case class DocumentationComment(member: Option[String] = None)

object DocumentationComment extends ShapeTag.Companion[DocumentationComment] {
  val id: ShapeId = ShapeId("smithy4s.example", "DocumentationComment")

  val hints: Hints = Hints(
    smithy.api.Documentation("We should be able to use comments in documentation /* */"),
  ).lazily

  // constructor using the original order from the spec
  private def make(member: Option[String]): DocumentationComment = DocumentationComment(member)

  implicit val schema: Schema[DocumentationComment] = struct(
    string.optional[DocumentationComment]("member", _.member).addHints(smithy.api.Documentation("/*")),
  )(make).withId(id).addHints(hints)
}
