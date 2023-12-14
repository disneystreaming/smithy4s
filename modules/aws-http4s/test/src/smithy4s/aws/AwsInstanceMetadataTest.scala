/*
 *  Copyright 2021-2023 Disney Streaming
 *
 *  Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     https://disneystreaming.github.io/TOST-1.0.txt
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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
