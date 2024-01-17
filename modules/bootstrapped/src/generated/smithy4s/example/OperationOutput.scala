package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class OperationOutput(optionalWithDefault: String = "optional-default", requiredWithDefault: String = "required-default", optionalHeaderWithDefault: String = "optional-header-with-default", requiredHeaderWithDefault: String = "required-header-with-default", optional: Option[String] = None, optionalHeader: Option[String] = None)

object OperationOutput extends ShapeTag.Companion[OperationOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "OperationOutput")

  val hints: Hints = Hints(
    smithy.api.Output(),
  ).lazily

  implicit val schema: Schema[OperationOutput] = struct(
    string.field[OperationOutput]("optionalWithDefault", _.optionalWithDefault).addHints(smithy.api.Default(smithy4s.Document.fromString("optional-default"))),
    string.required[OperationOutput]("requiredWithDefault", _.requiredWithDefault).addHints(smithy.api.Default(smithy4s.Document.fromString("required-default"))),
    string.field[OperationOutput]("optionalHeaderWithDefault", _.optionalHeaderWithDefault).addHints(smithy.api.Default(smithy4s.Document.fromString("optional-header-with-default")), smithy.api.HttpHeader("optional-header-with-default")),
    string.required[OperationOutput]("requiredHeaderWithDefault", _.requiredHeaderWithDefault).addHints(smithy.api.Default(smithy4s.Document.fromString("required-header-with-default")), smithy.api.HttpHeader("required-header-with-default")),
    string.optional[OperationOutput]("optional", _.optional),
    string.optional[OperationOutput]("optionalHeader", _.optionalHeader).addHints(smithy.api.HttpHeader("optional-header")),
  ){
    OperationOutput.apply
  }.withId(id).addHints(hints)
}
