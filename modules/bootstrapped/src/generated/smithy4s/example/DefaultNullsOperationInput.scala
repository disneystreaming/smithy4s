package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class DefaultNullsOperationInput(optionalWithDefault: String = "optional-default", requiredLabel: String = "required-label-with-default", requiredWithDefault: String = "required-default", optionalHeaderWithDefault: String = "optional-header-with-default", requiredHeaderWithDefault: String = "required-header-with-default", optionalQueryWithDefault: String = "optional-query-with-default", requiredQueryWithDefault: String = "required-query-with-default", optional: Option[String] = None, optionalHeader: Option[String] = None, optionalQuery: Option[String] = None)

object DefaultNullsOperationInput extends ShapeTag.Companion[DefaultNullsOperationInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "DefaultNullsOperationInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  ).lazily

  // constructor using the original order from the spec
  private def make(optional: Option[String], optionalWithDefault: String, requiredLabel: String, requiredWithDefault: String, optionalHeader: Option[String], optionalHeaderWithDefault: String, requiredHeaderWithDefault: String, optionalQuery: Option[String], optionalQueryWithDefault: String, requiredQueryWithDefault: String): DefaultNullsOperationInput = DefaultNullsOperationInput(optionalWithDefault, requiredLabel, requiredWithDefault, optionalHeaderWithDefault, requiredHeaderWithDefault, optionalQueryWithDefault, requiredQueryWithDefault, optional, optionalHeader, optionalQuery)

  implicit val schema: Schema[DefaultNullsOperationInput] = struct(
    string.optional[DefaultNullsOperationInput]("optional", _.optional),
    string.field[DefaultNullsOperationInput]("optionalWithDefault", _.optionalWithDefault).addHints(smithy.api.Default(smithy4s.Document.fromString("optional-default"))),
    string.required[DefaultNullsOperationInput]("requiredLabel", _.requiredLabel).addHints(smithy.api.Default(smithy4s.Document.fromString("required-label-with-default")), smithy.api.HttpLabel()),
    string.required[DefaultNullsOperationInput]("requiredWithDefault", _.requiredWithDefault).addHints(smithy.api.Default(smithy4s.Document.fromString("required-default"))),
    string.optional[DefaultNullsOperationInput]("optionalHeader", _.optionalHeader).addHints(smithy.api.HttpHeader("optional-header")),
    string.field[DefaultNullsOperationInput]("optionalHeaderWithDefault", _.optionalHeaderWithDefault).addHints(smithy.api.Default(smithy4s.Document.fromString("optional-header-with-default")), smithy.api.HttpHeader("optional-header-with-default")),
    string.required[DefaultNullsOperationInput]("requiredHeaderWithDefault", _.requiredHeaderWithDefault).addHints(smithy.api.Default(smithy4s.Document.fromString("required-header-with-default")), smithy.api.HttpHeader("required-header-with-default")),
    string.optional[DefaultNullsOperationInput]("optionalQuery", _.optionalQuery).addHints(smithy.api.HttpQuery("optional-query")),
    string.field[DefaultNullsOperationInput]("optionalQueryWithDefault", _.optionalQueryWithDefault).addHints(smithy.api.Default(smithy4s.Document.fromString("optional-query-with-default")), smithy.api.HttpQuery("optional-query-with-default")),
    string.field[DefaultNullsOperationInput]("requiredQueryWithDefault", _.requiredQueryWithDefault).addHints(smithy.api.Default(smithy4s.Document.fromString("required-query-with-default")), smithy.api.HttpQuery("required-query-with-default")),
  )(make).withId(id).addHints(hints)
}
