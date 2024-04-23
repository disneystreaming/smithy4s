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
import java.nio.file.Path
import java.nio.file.Paths

object Main extends IOApp.Simple {

  val keyName = "overlay/production/main/index.html"
  val newBytesName = "overlay/david/bytes-todelete.txt"
  val newFileName = "overlay/david/file-todelete.txt"
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
      s3 <- AwsClient.streamingClient(S3, awsEnv)
      res <- {
        s3.getObject(BucketName(bucketName), ObjectKey(keyName))
          .download
          .evalMap { output =>
            output.payload.compile.count
          }
      }
      _ <- IO.println(s"Downloaded $res from S3").toResource
      bytes = "bytes".getBytes()
      _ <- s3
        .putObject(BucketName(bucketName), ObjectKey(newBytesName))
        .upload(
          AwsStrictInput(
            fs2.Stream
              .emits(bytes.toSeq)
              .map(b => StreamingBlob(b)),
            bytes.length.toLong
          )
        )
        .toResource
    } yield ()
    program.use_
  }

  private def awsS3(): IO[Unit] = {
    import software.amazon.awssdk.http.SdkHttpClient
    import software.amazon.awssdk.http.apache.ProxyConfiguration
    import software.amazon.awssdk.http.apache.ApacheHttpClient
    import software.amazon.awssdk.services.s3.S3Client
    import software.amazon.awssdk.services.s3.model._
    import software.amazon.awssdk.regions.Region
    import software.amazon.awssdk.core.sync.RequestBody
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
    def getObject(s3: S3Client): IO[Unit] = {
      val getObjectRequest = GetObjectRequest
        .builder()
        .key(keyName)
        .bucket(bucketName)
        .build()
      val get = IO.blocking(s3.getObjectAsBytes(getObjectRequest).asByteArray())
      get.flatMap { bytes => IO.println(s"got ${bytes.size} bytes") }
    }
    def putObject(s3: S3Client, data: Array[Byte]): IO[Unit] = {
      val req =
        PutObjectRequest
          .builder()
          .key(newBytesName)
          // .checksumAlgorithm(ChecksumAlgorithm.SHA256)
          .bucket(bucketName)
          .build()
      val body = RequestBody.fromBytes(data)
      val put = IO.blocking(s3.putObject(req, body))
      put.flatMap { resp => IO.println(s"upload bytes ${resp.checksumSHA1()}") }
    }
    def putObjectFile(s3: S3Client, path: Path): IO[Unit] = {
      val req =
        PutObjectRequest.builder().key(newFileName).bucket(bucketName).build()
      val body = RequestBody.fromFile(path)
      val put = IO.blocking(s3.putObject(req, body))
      put.flatMap { resp => IO.println(s"upload file ${resp.checksumSHA1()}") }
    }
    client.flatMap(makeS3).use { s3 =>
      getObject(s3) *>
        putObject(s3, "my data is in s3".getBytes()) *>
        putObjectFile(
          s3,
          Paths.get("../../../LICENSE")
        )
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
            logBody = true,
            redactHeadersWhen = _ => false
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
