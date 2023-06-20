package aws.protocols

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

/** Provides the value in the 'Code' distinguishing field and HTTP response
  * code for an operation error.
  * @param code
  *   The value used to distinguish this error shape during serialization.
  * @param httpResponseCode
  *   The HTTP response code used on a response containing this error shape.
  */
final case class AwsQueryError(code: String, httpResponseCode: Int)
object AwsQueryError extends ShapeTag.Companion[AwsQueryError] {
  val id: ShapeId = ShapeId("aws.protocols", "awsQueryError")

  val hints: Hints = Hints(
    smithy.api.Documentation("Provides the value in the \'Code\' distinguishing field and HTTP response\ncode for an operation error."),
    smithy.api.Trait(selector = Some("structure [trait|error]"), structurallyExclusive = None, conflicts = None, breakingChanges = Some(List(smithy.api.TraitDiffRule(change = smithy.api.TraitChangeType.ANY.widen, severity = smithy.api.TraitChangeSeverity.ERROR.widen, path = None, message = None)))),
  )

  implicit val schema: Schema[AwsQueryError] = recursive(struct(
    string.required[AwsQueryError]("code", _.code).addHints(smithy.api.Documentation("The value used to distinguish this error shape during serialization."), smithy.api.Required()),
    int.required[AwsQueryError]("httpResponseCode", _.httpResponseCode).addHints(smithy.api.Documentation("The HTTP response code used on a response containing this error shape."), smithy.api.Required()),
  ){
    AwsQueryError.apply
  }.withId(id).addHints(hints))
}
