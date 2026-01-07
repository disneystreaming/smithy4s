package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class CheckQueryOutput(variants: Option[List[String]] = None, staticVariants: Option[List[String]] = None, kinds: Option[List[String]] = None, staticKinds: Option[List[String]] = None)

object CheckQueryOutput extends ShapeTag.Companion[CheckQueryOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "CheckQueryOutput")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(variants: Option[List[String]], staticVariants: Option[List[String]], kinds: Option[List[String]], staticKinds: Option[List[String]]): CheckQueryOutput = CheckQueryOutput(variants, staticVariants, kinds, staticKinds)

  implicit val schema: Schema[CheckQueryOutput] = struct(
    QueryVariants.underlyingSchema.optional[CheckQueryOutput]("variants", _.variants).addHints(smithy.api.Default(smithy4s.Document.nullDoc)),
    QueryVariants.underlyingSchema.optional[CheckQueryOutput]("staticVariants", _.staticVariants).addHints(smithy.api.Default(smithy4s.Document.nullDoc)),
    QueryKinds.underlyingSchema.optional[CheckQueryOutput]("kinds", _.kinds).addHints(smithy.api.Default(smithy4s.Document.nullDoc)),
    QueryKinds.underlyingSchema.optional[CheckQueryOutput]("staticKinds", _.staticKinds).addHints(smithy.api.Default(smithy4s.Document.nullDoc)),
  )(make).withId(id).addHints(hints)
}
