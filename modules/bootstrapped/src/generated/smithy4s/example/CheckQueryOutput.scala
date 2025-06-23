package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class CheckQueryOutput(variants: List[String] = List(), staticVariants: List[String] = List(), kinds: List[String] = List(), staticKinds: List[String] = List())

object CheckQueryOutput extends ShapeTag.Companion[CheckQueryOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "CheckQueryOutput")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(variants: List[String], staticVariants: List[String], kinds: List[String], staticKinds: List[String]): CheckQueryOutput = CheckQueryOutput(variants, staticVariants, kinds, staticKinds)

  implicit val schema: Schema[CheckQueryOutput] = struct(
    QueryVariants.underlyingSchema.field[CheckQueryOutput]("variants", _.variants).addHints(smithy.api.Default(smithy4s.Document.array())),
    QueryVariants.underlyingSchema.field[CheckQueryOutput]("staticVariants", _.staticVariants).addHints(smithy.api.Default(smithy4s.Document.array())),
    QueryKinds.underlyingSchema.field[CheckQueryOutput]("kinds", _.kinds).addHints(smithy.api.Default(smithy4s.Document.array())),
    QueryKinds.underlyingSchema.field[CheckQueryOutput]("staticKinds", _.staticKinds).addHints(smithy.api.Default(smithy4s.Document.array())),
  )(make).withId(id).addHints(hints)
}
