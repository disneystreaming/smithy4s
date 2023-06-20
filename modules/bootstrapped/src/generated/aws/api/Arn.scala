package aws.api

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.boolean
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

/** Specifies an ARN template for the resource.
  * @param template
  *   Defines the ARN template. The provided string contains URI-template
  *   style label placeholders that contain the name of one of the identifiers
  *   defined in the `identifiers` property of the resource. These labels can
  *   be substituted at runtime with the actual identifiers of the resource.
  *   Every identifier name of the resource MUST have corresponding label of
  *   the same name. Note that percent-encoding **is not** performed on these
  *   placeholder values; they are to be replaced literally. For relative ARN
  *   templates that have not set `absolute` to `true`, the template string
  *   contains only the resource part of the ARN (for example,
  *   `foo/{MyResourceId}`). Relative ARNs MUST NOT start with "/".
  * @param absolute
  *   Set to true to indicate that the ARN template contains a fully-formed
  *   ARN that does not need to be merged with the service. This type of ARN
  *   MUST be used when the identifier of a resource is an ARN or is based on
  *   the ARN identifier of a parent resource.
  * @param noRegion
  *   Set to true to specify that the ARN does not contain a region. If not
  *   set, or if set to false, the resolved ARN will contain a placeholder
  *   for the region. This can only be set to true if `absolute` is not set
  *   or is false.
  * @param noAccount
  *   Set to true to specify that the ARN does not contain an account ID. If
  *   not set, or if set to false, the resolved ARN will contain a placeholder
  *   for the customer account ID. This can only be set to true if absolute
  *   is not set or is false.
  */
final case class Arn(template: String, absolute: Option[Boolean] = None, noRegion: Option[Boolean] = None, noAccount: Option[Boolean] = None)
object Arn extends ShapeTag.Companion[Arn] {
  val id: ShapeId = ShapeId("aws.api", "arn")

  val hints: Hints = Hints(
    smithy.api.ExternalDocumentation(Map(smithy.api.NonEmptyString("Reference") -> smithy.api.NonEmptyString("https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html"))),
    smithy.api.Documentation("Specifies an ARN template for the resource."),
    smithy.api.Trait(selector = Some("resource"), structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val schema: Schema[Arn] = recursive(struct(
    string.required[Arn]("template", _.template).addHints(smithy.api.Documentation("Defines the ARN template. The provided string contains URI-template\nstyle label placeholders that contain the name of one of the identifiers\ndefined in the `identifiers` property of the resource. These labels can\nbe substituted at runtime with the actual identifiers of the resource.\nEvery identifier name of the resource MUST have corresponding label of\nthe same name. Note that percent-encoding **is not** performed on these\nplaceholder values; they are to be replaced literally. For relative ARN\ntemplates that have not set `absolute` to `true`, the template string\ncontains only the resource part of the ARN (for example,\n`foo/{MyResourceId}`). Relative ARNs MUST NOT start with \"/\"."), smithy.api.Required()),
    boolean.optional[Arn]("absolute", _.absolute).addHints(smithy.api.Documentation("Set to true to indicate that the ARN template contains a fully-formed\nARN that does not need to be merged with the service. This type of ARN\nMUST be used when the identifier of a resource is an ARN or is based on\nthe ARN identifier of a parent resource.")),
    boolean.optional[Arn]("noRegion", _.noRegion).addHints(smithy.api.Documentation("Set to true to specify that the ARN does not contain a region. If not\nset, or if set to false, the resolved ARN will contain a placeholder\nfor the region. This can only be set to true if `absolute` is not set\nor is false.")),
    boolean.optional[Arn]("noAccount", _.noAccount).addHints(smithy.api.Documentation("Set to true to specify that the ARN does not contain an account ID. If\nnot set, or if set to false, the resolved ARN will contain a placeholder\nfor the customer account ID. This can only be set to true if absolute\nis not set or is false.")),
  ){
    Arn.apply
  }.withId(id).addHints(hints))
}
