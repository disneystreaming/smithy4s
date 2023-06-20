package aws.api

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

/** An AWS service is defined using the `aws.api#service` trait. This trait
  * provides information about the service like the name used to generate AWS
  * SDK client classes and the namespace used in ARNs.
  * @param endpointPrefix
  *   The `endpointPrefix` property is a string value that identifies which
  *   endpoint in a given region should be used to connect to the service.
  *   For example, most services in the AWS standard partition have endpoints
  *   which follow the format: `{endpointPrefix}.{region}.amazonaws.com`. A
  *   service with the endpoint prefix example in the region us-west-2 might
  *   have the endpoint example.us-west-2.amazonaws.com.
  *   
  *   This value is not unique across services and is subject to change.
  *   Therefore, it MUST NOT be used for client naming or for any other
  *   purpose that requires a static, unique identifier. sdkId should be used
  *   for those purposes. Additionally, this value can be used to attempt to
  *   resolve endpoints.
  * @param cloudFormationName
  *   The `cloudFormationName` property is a string value that specifies the
  *   AWS CloudFormation service name (e.g., `ApiGateway`). When not set,
  *   this value defaults to the name of the service shape. This value is
  *   part of the CloudFormation resource type name that is automatically
  *   assigned to resources in the service (e.g., `AWS::<NAME>::resourceName`).
  * @param cloudTrailEventSource
  *   The `cloudTrailEventSource` property is a string value that defines the
  *   AWS customer-facing eventSource property contained in CloudTrail event
  *   records emitted by the service. If not specified, this value defaults
  *   to the `arnNamespace` plus `.amazonaws.com`.
  * @param arnNamespace
  *   The `arnNamespace` property is a string value that defines the ARN service
  *   namespace of the service (e.g., "apigateway"). This value is used in
  *   ARNs assigned to resources in the service. If not set, this value
  *   defaults to the lowercase name of the service shape.
  * @param sdkId
  *   The `sdkId` property is a required string value that specifies the AWS
  *   SDK service ID (e.g., "API Gateway"). This value is used for generating
  *   client names in SDKs and for linking between services.
  */
final case class Service(sdkId: String, arnNamespace: Option[ArnNamespace] = None, cloudFormationName: Option[CloudFormationName] = None, cloudTrailEventSource: Option[String] = None, endpointPrefix: Option[String] = None)
object Service extends ShapeTag.Companion[Service] {
  val id: ShapeId = ShapeId("aws.api", "service")

  val hints: Hints = Hints(
    smithy.api.Documentation("An AWS service is defined using the `aws.api#service` trait. This trait\nprovides information about the service like the name used to generate AWS\nSDK client classes and the namespace used in ARNs."),
    smithy.api.Trait(selector = Some("service"), structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val schema: Schema[Service] = recursive(struct(
    string.required[Service]("sdkId", _.sdkId).addHints(smithy.api.Documentation("The `sdkId` property is a required string value that specifies the AWS\nSDK service ID (e.g., \"API Gateway\"). This value is used for generating\nclient names in SDKs and for linking between services."), smithy.api.Required()),
    ArnNamespace.schema.optional[Service]("arnNamespace", _.arnNamespace).addHints(smithy.api.Documentation("The `arnNamespace` property is a string value that defines the ARN service\nnamespace of the service (e.g., \"apigateway\"). This value is used in\nARNs assigned to resources in the service. If not set, this value\ndefaults to the lowercase name of the service shape.")),
    CloudFormationName.schema.optional[Service]("cloudFormationName", _.cloudFormationName).addHints(smithy.api.Documentation("The `cloudFormationName` property is a string value that specifies the\nAWS CloudFormation service name (e.g., `ApiGateway`). When not set,\nthis value defaults to the name of the service shape. This value is\npart of the CloudFormation resource type name that is automatically\nassigned to resources in the service (e.g., `AWS::<NAME>::resourceName`).")),
    string.optional[Service]("cloudTrailEventSource", _.cloudTrailEventSource).addHints(smithy.api.Documentation("The `cloudTrailEventSource` property is a string value that defines the\nAWS customer-facing eventSource property contained in CloudTrail event\nrecords emitted by the service. If not specified, this value defaults\nto the `arnNamespace` plus `.amazonaws.com`.")),
    string.optional[Service]("endpointPrefix", _.endpointPrefix).addHints(smithy.api.Documentation("The `endpointPrefix` property is a string value that identifies which\nendpoint in a given region should be used to connect to the service.\nFor example, most services in the AWS standard partition have endpoints\nwhich follow the format: `{endpointPrefix}.{region}.amazonaws.com`. A\nservice with the endpoint prefix example in the region us-west-2 might\nhave the endpoint example.us-west-2.amazonaws.com.\n\nThis value is not unique across services and is subject to change.\nTherefore, it MUST NOT be used for client naming or for any other\npurpose that requires a static, unique identifier. sdkId should be used\nfor those purposes. Additionally, this value can be used to attempt to\nresolve endpoints.")),
  ){
    Service.apply
  }.withId(id).addHints(hints))
}
