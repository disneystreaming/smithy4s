package smithy4s.aws

import smithy4s.aws.kernel.AwsInstanceMetadata
import smithy4s.Blob

object AwsInstanceMetadataTest extends weaver.FunSuite {

  val codec =
    internals.AwsJsonCodecs.jsonDecoders.fromSchema(AwsInstanceMetadata.schema)

  test("decodes AwsInstanceMetadata correctly") {
    val result =
      codec.decode(Blob("""|{
                           |  "RoleArn":"arn:aws:iam::123:user:JohnDoe",
                           |  "AccessKeyId":"access-key",
                           |  "SecretAccessKey":"secret-key",
                           |  "Token":"token",
                           |  "Expiration":"2023-03-03T17:56:46Z"
                           |}""".stripMargin))
    assert(result.isRight)
  }
}
