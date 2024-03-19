/*
 *  Copyright 2021-2024 Disney Streaming
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
package sandbox
package aws

import cats.effect._
import org.http4s.client.middleware.RequestLogger
import org.http4s.ember.client.EmberClientBuilder
import smithy4s.aws._

object Main extends IOApp.Simple {

  val keyName = "overlay/production/main/index.html"
  val bucketName = "apiregistry-general"

  override def run: IO[Unit] = awsS3() *> smithy4sS3()

  private def smithy4sS3(): IO[Unit] = awsEnvironmentResource.use {
    awsEnvironment =>
      IO.println("hey")
  }

  private def awsS3(): IO[Unit] = {
    import software.amazon.awssdk.services.s3.S3Client
    import software.amazon.awssdk.services.s3.model._
    import software.amazon.awssdk.regions.Region

    val makeS3 = Resource.make(
      IO.delay(
        S3Client
          .builder()
          .region(Region.US_EAST_1)
          .build()
      )
    )(s3 => IO.delay(s3.close()))
    makeS3.use { s3 =>
      val getObjectRequest = GetObjectRequest
        .builder()
        .key(keyName)
        .bucket(bucketName)
        .build()
      val get = IO.blocking(s3.getObjectAsBytes(getObjectRequest).asByteArray())
      get.flatMap { bytes => IO.println(s"got ${bytes.size} bytes") }
    }
  }

  private val awsEnvironmentResource: Resource[IO, AwsEnvironment[IO]] =
    for {
      client <- EmberClientBuilder
        .default[IO]
        .build
        .map(
          RequestLogger.colored(
            logHeaders = true,
            logBody = true
          )
        )
      awsCredentialsProvider = new AwsCredentialsProvider[IO]
    } yield AwsEnvironment.make(
      client,
      IO.pure(AwsRegion.US_EAST_1),
      awsCredentialsProvider.defaultCredentialsFile.flatMap(
        awsCredentialsProvider
          .fromDisk(_, awsCredentialsProvider.getProfileFromEnv)
      ),
      IO.realTime.map(_.toSeconds).map(Timestamp(_, 0))
    )
}
