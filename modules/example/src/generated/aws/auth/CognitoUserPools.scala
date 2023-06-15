package aws.auth

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.struct

/** Configures an Amazon Cognito User Pools auth scheme.
  * @param providerArns
  *   A list of the Amazon Cognito user pool ARNs. Each element is of this
  *   format: `arn:aws:cognito-idp:{region}:{account_id}:userpool/{user_pool_id}`.
  */
final case class CognitoUserPools(providerArns: List[String])
object CognitoUserPools extends ShapeTag.Companion[CognitoUserPools] {
  val id: ShapeId = ShapeId("aws.auth", "cognitoUserPools")

  val hints: Hints = Hints(
    smithy.api.Internal(),
    smithy.api.Tags(List("internal")),
    smithy.api.Documentation("Configures an Amazon Cognito User Pools auth scheme."),
    smithy.api.AuthDefinition(traits = None),
    smithy.api.Trait(selector = Some("service"), structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val schema: Schema[CognitoUserPools] = recursive(struct(
    StringList.underlyingSchema.required[CognitoUserPools]("providerArns", _.providerArns).addHints(smithy.api.Documentation("A list of the Amazon Cognito user pool ARNs. Each element is of this\nformat: `arn:aws:cognito-idp:{region}:{account_id}:userpool/{user_pool_id}`."), smithy.api.Required()),
  ){
    CognitoUserPools.apply
  }.withId(id).addHints(hints))
}
