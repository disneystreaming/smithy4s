/*
 *  Copyright 2021-2022 Disney Streaming
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

import cats.effect._
import cats.syntax.all._
import fs2.io.file.Files
import org.http4s.EntityDecoder
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.syntax.all._
import smithy4s.aws.kernel.AWS_ACCESS_KEY_ID
import smithy4s.aws.kernel.AWS_PROFILE
import smithy4s.aws.kernel.AWS_SECRET_ACCESS_KEY
import smithy4s.aws.kernel.AWS_SESSION_TOKEN
import smithy4s.aws.kernel.AwsInstanceMetadata
import smithy4s.aws.kernel.AwsTemporaryCredentials
import smithy4s.aws.kernel.SysEnv
import smithy4s.http4s.kernel.EntityDecoders

import scala.concurrent.duration._

object AwsCredentialsProvider {

  def default[F[_]: Temporal: Files](
      httpClient: Client[F]
  ): Resource[F, F[AwsCredentials]] = {
    val provider = new AwsCredentialsProvider[F]
    provider.default(httpClient)
  }

}

class AwsCredentialsProvider[F[_]](implicit F: Temporal[F]) {

  private val httpMediaType = smithy4s.http.HttpMediaType("application/json")
  implicit val awsInstanceMetadataDecoder
      : EntityDecoder[F, AwsInstanceMetadata] =
    internals.AwsJsonCodecs.jsonPayloadCodecs
      .mapK(EntityDecoders.fromPayloadCodecK[F](httpMediaType))
      .fromSchema(AwsInstanceMetadata.schema)

  def default(
      httpClient: Client[F]
  )(implicit files: Files[F]): Resource[F, F[AwsCredentials]] = {
    val _fromDisk =
      defaultCredentialsFile.flatMap(fromDisk(_, getProfileFromEnv))

    Resource
      .eval(fromEnv)
      .map(F.pure)
      // TODO: Ensure minimal delay when these endpoints don't exist, e.g.
      // when running locally.
      .orElse(refreshing(fromECS(httpClient)))
      .orElse(refreshing(fromEC2(httpClient)))
      .orElse(Resource.eval(_fromDisk).map(F.pure))
  }

  def defaultCredentialsFile(implicit files: Files[F]): F[fs2.io.file.Path] =
    SysEnv
      .envValue("HOME")
      .liftTo[F]
      .flatMap { home =>
        val path = fs2.io.file.Path(s"$home/.aws/credentials")
        Files[F]
          .exists(path)
          .ifM(
            path.pure[F],
            F.raiseError(
              AwsCredentialsFileException(
                s"Credentials file not found at '$path'"
              )
            )
          )
      }

  def fromDisk(
      path: fs2.io.file.Path,
      profile: Option[String]
  )(implicit files: Files[F]): F[AwsCredentials] = {
    AwsCredentialsFile.fromDisk(path, profile)
  }

  val fromEnv: F[AwsCredentials] = {
    val either = for {
      keyId <- SysEnv.envValue(AWS_ACCESS_KEY_ID)
      accessKey <- SysEnv.envValue(AWS_SECRET_ACCESS_KEY)
      session = SysEnv.envValue(AWS_SESSION_TOKEN).toOption
    } yield AwsCredentials.Default(keyId, accessKey, session)
    either match {
      case Right(value) => F.pure(value)
      case Left(t)      => F.raiseError(t)
    }
  }

  def getProfileFromEnv: Option[String] =
    SysEnv.envValue(AWS_PROFILE).toOption

  val AWS_CONTAINER_CREDENTIALS_RELATIVE_URI =
    "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI"

  val AWS_EC2_METADATA_URI =
    uri"http://169.254.169.254/latest/meta-data/iam/security-credentials/"

  val AWS_ECS_METADATA_BASE_URI =
    uri"http://169.254.170.2"

  def fromEC2(httpClient: Client[F]): F[AwsTemporaryCredentials] =
    for {
      roleName <- httpClient.expect[String](AWS_EC2_METADATA_URI)
      metadataRes <- httpClient.expect[AwsInstanceMetadata](
        AWS_EC2_METADATA_URI.addSegment(roleName)
      )
    } yield metadataRes

  def fromECS(httpClient: Client[F]): F[AwsTemporaryCredentials] =
    for {
      path <- SysEnv.envValue(AWS_CONTAINER_CREDENTIALS_RELATIVE_URI).liftTo[F]
      metadataRes <- httpClient.expect[AwsInstanceMetadata](
        AWS_ECS_METADATA_BASE_URI.withPath(Uri.Path.unsafeFromString(path))
      )
    } yield metadataRes

  def refreshing(
      get: F[AwsTemporaryCredentials]
  ): Resource[F, F[AwsCredentials]] = {
    def refreshLoop(ref: Ref[F, AwsTemporaryCredentials]): F[Unit] = {
      for {
        lastCredentials <- ref.get
        now <- Clock[F].realTime.map(_.toSeconds)
        expires = lastCredentials.expiration.epochSecond
        delay = expires - now - 300
        _ <- Temporal[F].sleep(delay.seconds)
        newCredentials <- get
        _ <- ref.set(newCredentials)
      } yield ()
    }.foreverM

    for {
      startingMetadata <- Resource.eval(get)
      currentCredsRef <- Resource.eval(Ref[F].of(startingMetadata))
      _ <- F.background(refreshLoop(currentCredsRef))
    } yield currentCredsRef.get.widen
  }

}
