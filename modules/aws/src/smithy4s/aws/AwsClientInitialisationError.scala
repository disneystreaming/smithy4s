package smithy4s.aws

import smithy4s.ShapeId
import smithy4s.ShapeTag

sealed trait AwsClientInitialisationError extends Exception
object AwsClientInitialisationError {
  case class NotAws(serviceId: ShapeId)
      extends Exception(s"${serviceId.show} is not an AWS service")
      with AwsClientInitialisationError

  case class NoEndpointPrefix(awsService: _root_.aws.api.Service)
      extends Exception(s"No endpoint prefix for $awsService")
      with AwsClientInitialisationError

  case class UnsupportedProtocol(
      serviceId: ShapeId,
      knownProtocols: List[ShapeTag[_]]
  ) extends Exception(
        s"AWS protocol used by ${serviceId.show} is not yet supported"
      )
      with AwsClientInitialisationError

}
