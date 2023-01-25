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

import cats.effect.Async
import cats.effect.Clock
import cats.effect.Concurrent
import cats.effect.Ref
import cats.effect.Resource
import cats.effect.Temporal
import cats.MonadThrow
import cats.syntax.all._
import fs2.io.file.Files
import smithy4s.aws.kernel.AWS_ACCESS_KEY_ID
import smithy4s.aws.kernel.AWS_PROFILE
import smithy4s.aws.kernel.AWS_SECRET_ACCESS_KEY
import smithy4s.aws.kernel.AWS_SESSION_TOKEN
import smithy4s.aws.kernel.AwsCredentials
import smithy4s.aws.kernel.AwsInstanceMetadata
import smithy4s.aws.kernel.AwsTemporaryCredentials
import smithy4s.aws.kernel.SysEnv
import smithy4s.http.HttpMethod

import scala.concurrent.duration._

object AwsCredentialsProvider {

  def default[F[_]](
      httpClient: SimpleHttpClient[F]
  )(implicit F: Async[F]): Resource[F, F[AwsCredentials]] = {
    val env = Resource.eval(fromEnv[F]).map(F.pure)
    val _fromDisk =
      defaultCredentialsFile.flatMap(path =>
        fromDisk[F](path, getProfileFromEnv)
      )
    env
      .orElse(refreshing[F](fromEC2(httpClient)))
      .orElse(Resource.eval(_fromDisk).map(F.pure))
  }

  def fromEnv[F[_]](implicit F: MonadThrow[F]): F[AwsCredentials] = {
    val either: Either[Throwable, AwsCredentials] = for {
      keyId <- SysEnv.envValue(AWS_ACCESS_KEY_ID)
      accessKey <- SysEnv.envValue(AWS_SECRET_ACCESS_KEY)
      session = SysEnv.envValue(AWS_SESSION_TOKEN).toOption
    } yield AwsCredentials.Default(keyId, accessKey, session)
    either.liftTo[F]
  }

  def getProfileFromEnv: Option[String] =
    SysEnv.envValue(AWS_PROFILE).toOption

  def defaultCredentialsFile[F[_]: Files: MonadThrow]: F[fs2.io.file.Path] =
    SysEnv
      .envValue("HOME")
      .liftTo[F]
      .flatMap { home =>
        val path = fs2.io.file.Path(s"$home/.aws/credentials")
        Files[F]
          .exists(path)
          .ifM(
            path.pure[F],
            MonadThrow[F].raiseError(AwsCredentialsFileException("rip"))
          )
      }

  def fromDisk[F[_]: Concurrent: Files](
      path: fs2.io.file.Path,
      profile: Option[String]
  ): F[AwsCredentials] = {
    AwsCredentialsFile.fromDisk(path, profile)
  }

  val AWS_CONTAINER_CREDENTIALS_RELATIVE_URI =
    "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI"

  val AWS_EC2_METADATA_URI =
    "http://169.254.169.254/latest/meta-data/iam/security-credentials/"

  val codecAPI = new json.AwsJsonCodecAPI()

  val instanceMetadataCodec = codecAPI.compileCodec(AwsInstanceMetadata.schema)

  def fromEC2[F[_]: MonadThrow](
      httpClient: SimpleHttpClient[F]
  ): F[AwsTemporaryCredentials] =
    for {
      roleRes <- httpClient.run(
        HttpRequest.Raw(HttpMethod.GET, AWS_EC2_METADATA_URI)
      )
      roleName <- utf8String[F](roleRes.body)
      metadataRes <- httpClient.run(
        HttpRequest.Raw(HttpMethod.GET, AWS_EC2_METADATA_URI + roleName)
      )
      maybeCreds = codecAPI.decodeFromByteArray(
        instanceMetadataCodec,
        metadataRes.body
      )
      creds <- MonadThrow[F].fromEither(maybeCreds)
    } yield creds

  def fromECS[F[_]: MonadThrow](
      httpClient: SimpleHttpClient[F]
  ): F[AwsTemporaryCredentials] =
    for {
      response <- httpClient.run(
        HttpRequest.Raw(HttpMethod.GET, AWS_EC2_METADATA_URI)
      )
      maybeCreds = codecAPI.decodeFromByteArray(
        instanceMetadataCodec,
        response.body
      )
      creds <- MonadThrow[F].fromEither(maybeCreds)
    } yield creds

  def refreshing[F[_]](
      get: F[AwsTemporaryCredentials]
  )(implicit F: Temporal[F]): Resource[F, F[AwsCredentials]] = {
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
