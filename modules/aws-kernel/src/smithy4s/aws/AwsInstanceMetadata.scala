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

package smithy4s
package aws.kernel

import smithy4s.schema.Schema._

case class AwsInstanceMetadata(
    accessKeyId: String,
    expiration: Timestamp,
    secretAccessKey: String,
    token: String
) extends AwsTemporaryCredentials {
  def sessionToken = Some(token)
}

object AwsInstanceMetadata {

  implicit val schema: Schema[AwsInstanceMetadata] = {
    val accessKeyIdField =
      string.required[AwsInstanceMetadata]("AccessKeyId", _.accessKeyId)
    val expirationField =
      timestamp.required[AwsInstanceMetadata]("Expiration", _.expiration)
    val secretAccessKeyField =
      string.required[AwsInstanceMetadata]("SecretAccessKey", _.accessKeyId)
    val tokenField =
      string.required[AwsInstanceMetadata]("Token", _.accessKeyId)
    struct(accessKeyIdField, expirationField, secretAccessKeyField, tokenField)(
      AwsInstanceMetadata.apply
    )
  }

}
