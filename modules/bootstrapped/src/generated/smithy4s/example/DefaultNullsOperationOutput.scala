package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class DefaultNullsOperationOutput(optionalWithDefault: String = "optional-default", requiredWithDefault: String = "required-default", optionalHeaderWithDefault: String = "optional-header-with-default", requiredHeaderWithDefault: String = "required-header-with-default", optional: Option[String] = None, optionalHeader: Option[String] = None)

object DefaultNullsOperationOutput extends ShapeTag.Companion[DefaultNullsOperationOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "DefaultNullsOperationOutput")

  val hints: Hints = Hints(
    smithy.api.Output(),
  ).lazily

  // constructor using the original order from the spec
  private def make(optional: Option[String], optionalWithDefault: String, requiredWithDefault: String, optionalHeader: Option[String], optionalHeaderWithDefault: String, requiredHeaderWithDefault: String): DefaultNullsOperationOutput = DefaultNullsOperationOutput(optionalWithDefault, requiredWithDefault, optionalHeaderWithDefault, requiredHeaderWithDefault, optional, optionalHeader)

  implicit val schema: Schema[DefaultNullsOperationOutput] = struct(
    string.optional[DefaultNullsOperationOutput]("optional", _.optional),
    string.field[DefaultNullsOperationOutput]("optionalWithDefault", _.optionalWithDefault).addHints(smithy.api.Default(smithy4s.Document.fromString("optional-default"))),
    string.required[DefaultNullsOperationOutput]("requiredWithDefault", _.requiredWithDefault).addHints(smithy.api.Default(smithy4s.Document.fromString("required-default"))),
    string.optional[DefaultNullsOperationOutput]("optionalHeader", _.optionalHeader).addHints(smithy.api.HttpHeader("optional-header")),
    string.field[DefaultNullsOperationOutput]("optionalHeaderWithDefault", _.optionalHeaderWithDefault).addHints(smithy.api.Default(smithy4s.Document.fromString("optional-header-with-default")), smithy.api.HttpHeader("optional-header-with-default")),
    string.required[DefaultNullsOperationOutput]("requiredHeaderWithDefault", _.requiredHeaderWithDefault).addHints(smithy.api.Default(smithy4s.Document.fromString("required-header-with-default")), smithy.api.HttpHeader("required-header-with-default")),
  )(make).withId(id).addHints(hints)
}
