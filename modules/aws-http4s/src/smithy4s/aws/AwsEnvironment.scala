/*
 *  Copyright 2021-2025 Disney Streaming
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

import cats.effect.Resource
import cats.effect.Temporal
import cats.syntax.all._
import fs2.io.file.Files
import org.http4s.client.Client
import smithy4s.Timestamp
import smithy4s.Endpoint

// scalafmt: {maxColumn = 120}
trait AwsEnvironment[F[_]] {
  def credentials: F[AwsCredentials]
  def region: F[AwsRegion]
  def timestamp: F[Timestamp]
  def httpClient: Client[F]
  def endpointMiddleware: Endpoint.Middleware[Client[F]]

  // Fluent builders
  def withMiddleware(middleware: Endpoint.Middleware[Client[F]]): AwsEnvironment[F]
  def withTimestamp(timestamp: F[Timestamp]): AwsEnvironment[F]
}

object AwsEnvironment {

  def default[F[_]: Files](
      client: Client[F],
      region: AwsRegion
  )(implicit F: Temporal[F]): Resource[F, AwsEnvironment[F]] =
    AwsCredentialsProvider.default[F](client).map { credentialsF =>
      make(
        client,
        F.pure(region),
        credentialsF,
        // note: fromEpochMilli would be nice
        F.realTime.map(_.toSeconds).map(Timestamp(_, 0))
      )
    }

  def make[F[_]](
      client: Client[F],
      awsRegion: F[AwsRegion],
      creds: F[AwsCredentials],
      time: F[Timestamp]
  ): AwsEnvironment[F] = new Impl[F](
    credentials = creds,
    region = awsRegion,
    timestamp = time,
    httpClient = client,
    endpointMiddleware = Endpoint.Middleware.noop
  )

  private case class Impl[F[_]](
      credentials: F[AwsCredentials],
      region: F[AwsRegion],
      timestamp: F[Timestamp],
      httpClient: Client[F],
      endpointMiddleware: Endpoint.Middleware[Client[F]]
  ) extends AwsEnvironment[F] {
    def withMiddleware(middleware: Endpoint.Middleware[Client[F]]): AwsEnvironment[F] =
      copy(endpointMiddleware = middleware)

    def withTimestamp(timestamp: F[Timestamp]): AwsEnvironment[F] = copy(timestamp = timestamp)
  }

}
