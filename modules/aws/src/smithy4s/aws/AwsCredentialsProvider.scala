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

import cats.MonadThrow
import cats.effect.Clock
import cats.effect.Resource
import cats.effect.Temporal
import cats.effect.kernel.Ref
import cats.syntax.all._
import smithy4s.aws.kernel.AWS_ACCESS_KEY_ID
import smithy4s.aws.kernel.AWS_SECRET_ACCESS_KEY
import smithy4s.aws.kernel.AWS_SESSION_TOKEN
import smithy4s.aws.kernel.AwsInstanceMetadata
import smithy4s.aws.kernel.AwsTemporaryCredentials
import smithy4s.aws.kernel.SysEnv
import smithy4s.http.HttpMethod

import scala.concurrent.duration._

object AwsCredentialsProvider {

  def default[F[_]](
      httpClient: SimpleHttpClient[F]
  )(implicit F: Temporal[F]): Resource[F, F[AwsCredentials]] = {
    Resource
      .eval(fromEnv[F])
      .map(F.pure)
      .orElse(refreshing[F](fromECS(httpClient)))
      .orElse(refreshing[F](fromEC2(httpClient)))
  }

  def fromEnv[F[_]](implicit F: MonadThrow[F]): F[AwsCredentials] = {
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

  val AWS_CONTAINER_CREDENTIALS_RELATIVE_URI =
    "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI"

  val AWS_EC2_METADATA_URI =
    "http://169.254.169.254/latest/meta-data/iam/security-credentials/"

  val instanceMetadataCodec =
    json.awsJson.compileCodec(AwsInstanceMetadata.schema)

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
      maybeCreds = json.awsJson.decodeFromByteArray(
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
      maybeCreds = json.awsJson.decodeFromByteArray(
        instanceMetadataCodec,
        response.body
      )
      creds <- MonadThrow[F].fromEither(maybeCreds)
    } yield creds

  def refreshing[F[_]](
      get: F[AwsTemporaryCredentials]
  )(implicit F: Temporal[F]): Resource[F, F[AwsCredentials]] = {
    def refreshLoop(ref: Ref[F, AwsTemporaryCredentials]): F[Unit] =
      for {
        lastCredentials <- ref.get
        now <- Clock[F].realTime.map(_.toSeconds)
        expires = lastCredentials.expiration.epochSecond
        delay = expires - now - 300
        _ <- Temporal[F].sleep(delay.seconds)
        newCredentials <- get
        _ <- ref.set(newCredentials)
        _ <- refreshLoop(ref)
      } yield ()

    for {
      startingMetadata <- Resource.eval(get)
      currentCredsRef <- Resource.eval(Ref[F].of(startingMetadata))
      _ <- F.background(refreshLoop(currentCredsRef))
    } yield currentCredsRef.get.widen
  }

}
