package aws.auth

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

/** Signature Version 4 is the process to add authentication information to
  * AWS requests sent by HTTP. For security, most requests to AWS must be
  * signed with an access key, which consists of an access key ID and secret
  * access key. These two keys are commonly referred to as your security
  * credentials.
  * @param name
  *   The signature version 4 service signing name to use in the credential
  *   scope when signing requests. This value SHOULD match the `arnNamespace`
  *   property of the `aws.api#service-trait`.
  */
final case class Sigv4(name: String)
object Sigv4 extends ShapeTag.Companion[Sigv4] {
  val id: ShapeId = ShapeId("aws.auth", "sigv4")

  val hints: Hints = Hints(
    smithy.api.ExternalDocumentation(Map(smithy.api.NonEmptyString("Reference") -> smithy.api.NonEmptyString("https://docs.aws.amazon.com/general/latest/gr/signature-version-4.html"))),
    smithy.api.Documentation("Signature Version 4 is the process to add authentication information to\nAWS requests sent by HTTP. For security, most requests to AWS must be\nsigned with an access key, which consists of an access key ID and secret\naccess key. These two keys are commonly referred to as your security\ncredentials."),
    smithy.api.AuthDefinition(traits = Some(List(smithy.api.TraitShapeId("aws.auth#unsignedPayload")))),
    smithy.api.Trait(selector = Some("service"), structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val schema: Schema[Sigv4] = recursive(struct(
    string.validated(smithy.api.Length(min = Some(1L), max = None)).required[Sigv4]("name", _.name).addHints(smithy.api.ExternalDocumentation(Map(smithy.api.NonEmptyString("Reference") -> smithy.api.NonEmptyString("https://docs.aws.amazon.com/general/latest/gr/sigv4-create-string-to-sign.html"))), smithy.api.Required(), smithy.api.Documentation("The signature version 4 service signing name to use in the credential\nscope when signing requests. This value SHOULD match the `arnNamespace`\nproperty of the `aws.api#service-trait`.")),
  ){
    Sigv4.apply
  }.withId(id).addHints(hints))
}
