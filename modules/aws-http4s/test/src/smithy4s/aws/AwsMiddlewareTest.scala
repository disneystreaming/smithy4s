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

import weaver._
import cats.effect.IO
import org.http4s.Request
import org.http4s.Response
import org.http4s.Status
import org.http4s.client.Client
import com.amazonaws.dynamodb.DynamoDB
import _root_.smithy4s.Endpoint
import _root_.smithy4s.Service
import org.http4s.Headers
import org.typelevel.ci.CIString

object AwsMiddlewareTest extends SimpleIOSuite with Compat {

  test("Endpoint middleware can be applied on AWS Clients") {
    val epMiddleware = new Endpoint.Middleware[Client[IO]] {
      def prepare[Alg[_[_, _, _, _, _]]](
          service: Service[Alg]
      )(endpoint: service.Endpoint[_, _, _, _, _]): Client[IO] => Client[IO] =
        underlying =>
          Client { req =>
            underlying.run(
              req.transformHeaders(_ ++ Headers("X-Service" -> service.id.name))
            )
          }
    }
    val test = for {
      ref <- IO.ref[Option[Request[IO]]](None).toResource
      httpClient = Client[IO](req =>
        ref.set(Some(req)).as(Response[IO](Status.Forbidden)).toResource
      )
      awsEnv <- AwsEnvironment
        .default(httpClient, AwsRegion.US_EAST_1)
        .map(_.withMiddleware(epMiddleware))
      awsClient <- AwsClient(DynamoDB, awsEnv)
      _ <- awsClient.listTables().attempt.toResource
      interceptedHeaders <- ref.get
        .map(_.map(_.headers).getOrElse(Headers.empty))
        .toResource
    } yield {
      val xServiceHeader =
        interceptedHeaders.get(CIString("X-Service")).map(_.head.value)
      expect.same(xServiceHeader, Some("DynamoDB_20120810"))
    }
    test.use(IO.pure)
  }

}
