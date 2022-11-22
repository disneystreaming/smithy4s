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

package smithy4s.http4s

import cats.effect.IO
import io.circe.syntax._
import org.http4s.Method
import org.http4s.Request
import org.http4s.Status
import org.http4s.circe.CirceEntityCodec._
import org.http4s.client.Client
import org.http4s.implicits._
import smithy4s.example.EchoService
import weaver._

object Http4sNoErrorsServerSpec extends SimpleIOSuite {
  private val routes = SimpleRestJsonBuilder
    .routes(new EchoService[IO] {
      def echo(
          pathParam: String,
          queryParam: Option[String],
          body: Option[String]
      ): IO[Unit] = IO.unit
    })
    .make
    .toTry
    .get

  private val client = Client.fromHttpApp(routes.orNotFound)

  test("path param failing refinement results in a BadRequest") {
    client
      .status(
        Request[IO](
          method = Method.POST,
          uri = uri"/echo/test"
        )
      )
      .map(assert.eql(_, Status.BadRequest))
  }

  test("query param failing refinement results in a BadRequest") {
    client
      .status(
        Request[IO](
          method = Method.POST,
          uri = uri"/echo/test-long?queryParam=test"
        )
      )
      .map(assert.eql(_, Status.BadRequest))
  }

  test("body failing refinement results in a BadRequest") {
    client
      .status(
        Request[IO](
          method = Method.POST,
          uri = uri"/echo/test-long"
        ).withEntity("test".asJson)
      )
      .map(assert.eql(_, Status.BadRequest))
  }
}
