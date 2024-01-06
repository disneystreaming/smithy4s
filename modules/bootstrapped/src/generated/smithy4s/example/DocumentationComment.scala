package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

/** We should be able to use comments in documentation /&ast; \*\/
  * @param member
  *   /&ast;
  */
final case class DocumentationComment(member: Option[String] = None)

object DocumentationComment extends ShapeTag.Companion[DocumentationComment] {
  val id: ShapeId = ShapeId("smithy4s.example", "DocumentationComment")

  val hints: Hints = Hints(
    smithy.api.Documentation("We should be able to use comments in documentation /* */"),
  )

  implicit val schema: Schema[DocumentationComment] = struct(
    string.optional[DocumentationComment]("member", _.member).addHints(smithy.api.Documentation("/*")),
  ){
    DocumentationComment.apply
  }.withId(id).addHints(hints)
}
