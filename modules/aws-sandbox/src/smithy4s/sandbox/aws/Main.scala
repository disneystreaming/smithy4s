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
import com.amazonaws.{s3 => smithyS3}
import smithy4s.aws._
import java.net.URI

object Main extends IOApp.Simple {

  val keyName = "overlay/production/main/index.html"
  val bucketName = "apiregistry-general"

  override def run: IO[Unit] =
    awsS3().attempt.flatMap(print) *> smithy4sS3().attempt.flatMap(print)

  private def print[B](either: Either[Throwable, B]): IO[Unit] =
    either match {
      case Left(value) =>
        IO.consoleForIO.printStackTrace(value)
      case Right(value) => IO.println(value)
    }

  private def smithy4sS3(): IO[Unit] = {
    import smithyS3._
    val program = for {
      awsEnv <- awsEnvironmentResource
      s3 <- AwsClient(S3, awsEnv)
      res <- {
        // F[GetObjectRequest, S3Operation.GetObjectError, GetObjectOutput, Nothing, StreamingBlob]
        s3.getObject(BucketName(bucketName), ObjectKey(keyName)).map { output =>
          IO.println(s"hey s3 ${output}")
        }
      }.toResource
    } yield res
    program.use_
  }

  private def awsS3(): IO[Unit] = {
    import software.amazon.awssdk.http.SdkHttpClient
    import software.amazon.awssdk.http.apache.ProxyConfiguration
    import software.amazon.awssdk.http.apache.ApacheHttpClient
    import software.amazon.awssdk.services.s3.S3Client
    import software.amazon.awssdk.services.s3.model._
    import software.amazon.awssdk.regions.Region
    import software.amazon.awssdk.utils.AttributeMap
    import software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES

    val pc = ProxyConfiguration
      .builder()
      .endpoint(URI.create("http://localhost:8080"))
      .build()

    val client =
      Resource.make(
        IO.delay(
          ApacheHttpClient
            .builder()
            .proxyConfiguration(pc)
            .buildWithDefaults(
              AttributeMap
                .builder()
                .put(TRUST_ALL_CERTIFICATES, java.lang.Boolean.TRUE)
                .build()
            )
        )
      )(c => IO.delay(c.close()))

    def makeS3(client: SdkHttpClient) = Resource.make(
      IO.delay(
        S3Client
          .builder()
          .httpClient(client)
          .region(Region.US_EAST_1)
          .build()
      )
    )(s3 => IO.delay(s3.close()))
    client.flatMap(makeS3).use { s3 =>
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
