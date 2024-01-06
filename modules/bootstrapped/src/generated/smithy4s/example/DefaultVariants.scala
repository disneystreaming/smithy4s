package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class DefaultVariants(req: String, reqDef: String = "default", optDef: String = "default", opt: Option[String] = None)

object DefaultVariants extends ShapeTag.Companion[DefaultVariants] {
  val id: ShapeId = ShapeId("smithy4s.example", "DefaultVariants")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[DefaultVariants] = struct(
    string.required[DefaultVariants]("req", _.req),
    string.required[DefaultVariants]("reqDef", _.reqDef).addHints(smithy.api.Default(_root_.smithy4s.Document.fromString("default"))),
    string.field[DefaultVariants]("optDef", _.optDef).addHints(smithy.api.Default(_root_.smithy4s.Document.fromString("default"))),
    string.optional[DefaultVariants]("opt", _.opt),
  ){
    DefaultVariants.apply
  }.withId(id).addHints(hints)
}
