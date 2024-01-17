package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class OperationInput(optionalWithDefault: String = "optional-default", requiredLabel: String = "required-label-with-default", requiredWithDefault: String = "required-default", optionalHeaderWithDefault: String = "optional-header-with-default", requiredHeaderWithDefault: String = "required-header-with-default", optionalQueryWithDefault: String = "optional-query-with-default", requiredQueryWithDefault: String = "required-query-with-default", optional: Option[String] = None, optionalHeader: Option[String] = None, optionalQuery: Option[String] = None)

object OperationInput extends ShapeTag.Companion[OperationInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "OperationInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  ).lazily

  implicit val schema: Schema[OperationInput] = struct(
    string.field[OperationInput]("optionalWithDefault", _.optionalWithDefault).addHints(smithy.api.Default(smithy4s.Document.fromString("optional-default"))),
    string.required[OperationInput]("requiredLabel", _.requiredLabel).addHints(smithy.api.Default(smithy4s.Document.fromString("required-label-with-default")), smithy.api.HttpLabel()),
    string.required[OperationInput]("requiredWithDefault", _.requiredWithDefault).addHints(smithy.api.Default(smithy4s.Document.fromString("required-default"))),
    string.field[OperationInput]("optionalHeaderWithDefault", _.optionalHeaderWithDefault).addHints(smithy.api.Default(smithy4s.Document.fromString("optional-header-with-default")), smithy.api.HttpHeader("optional-header-with-default")),
    string.required[OperationInput]("requiredHeaderWithDefault", _.requiredHeaderWithDefault).addHints(smithy.api.Default(smithy4s.Document.fromString("required-header-with-default")), smithy.api.HttpHeader("required-header-with-default")),
    string.field[OperationInput]("optionalQueryWithDefault", _.optionalQueryWithDefault).addHints(smithy.api.Default(smithy4s.Document.fromString("optional-query-with-default")), smithy.api.HttpQuery("optional-query-with-default")),
    string.field[OperationInput]("requiredQueryWithDefault", _.requiredQueryWithDefault).addHints(smithy.api.Default(smithy4s.Document.fromString("required-query-with-default")), smithy.api.HttpQuery("required-query-with-default")),
    string.optional[OperationInput]("optional", _.optional),
    string.optional[OperationInput]("optionalHeader", _.optionalHeader).addHints(smithy.api.HttpHeader("optional-header")),
    string.optional[OperationInput]("optionalQuery", _.optionalQuery).addHints(smithy.api.HttpQuery("optional-query")),
  ){
    OperationInput.apply
  }.withId(id).addHints(hints)
}
