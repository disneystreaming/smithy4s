package smithy4s.aws

import weaver._
import smithy4s.example.aws.MyThing

object ClientPrepareTest extends FunSuite {
  test(
    "Using a service without a supported protocol gives you a list of supported ones"
  ) {
    AwsClient.prepare(MyThing) match {
      case Left(p) =>
        assert.same(
          p,
          AwsClient.InitialisationError.UnsupportedProtocol(
            MyThing.id,
            List(
              aws.protocols.AwsJson1_0.getTag,
              aws.protocols.AwsJson1_1.getTag
            )
          )
        )

      case Right(other) =>
        failure(s"Expected Left, got Right instead: $other")
    }
  }
}
