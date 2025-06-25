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

package smithy4s.tests

import cats.effect._
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import weaver._
import org.http4s.client.Client
import org.http4s.Uri
import cats.Show
import org.http4s.Request
import smithy4s.routing.RoutingService

abstract class RoutingSpec
    extends IOSuite
    with Http4sDsl[IO]
    with Http4sClientDsl[IO] {

  type Res = (Client[IO], Uri)

  def runServer(
      routingService: RoutingService[IO],
      errorAdapter: PartialFunction[Throwable, Throwable]
  ): Resource[IO, Res]

  override def sharedResource: Resource[IO, Res] =
    runServer(
      new RoutingServiceImpl(),
      PartialFunction.empty[Throwable, Throwable]
    )

  routerTest("/abc") { (client, uri) =>
    for {
      res <- client.send[String](GET((uri / "abc")))
    } yield {
      val (code, body) = res
      expect.same(code, 200) && expect(body == "\"abc\"")
    }
  }

  routerTest("/abc/def") { (client, uri) =>
    for {
      res <- client.send[String](GET((uri / "abc" / "def")))
    } yield {
      val (code, body) = res
      expect.same(code, 200) && expect(body == "\"abcDef\"")
    }
  }

  routerTest("/abc/{def}") { (client, uri) =>
    for {
      res <- client.send[String](GET((uri / "abc" / "notDef")))
    } yield {
      val (code, body) = res
      expect.same(code, 200) && expect(body == "\"abcLabel\"")
    }
  }

  routerTest("/abc/{def+}") { (client, uri) =>
    for {
      res <- client.send[String](GET((uri / "abc" / "def" / "def")))
    } yield {
      val (code, body) = res
      expect.same(code, 200) && expect(body == "\"greedyAbcDef\"")
    }
  }

  def routerTest(testName: TestName)(
      f: (Client[IO], Uri) => IO[Expectations]
  ) = test(testName)((res: Res) => f(res._1, res._2))

  implicit class ClientOps(client: Client[IO]) {
    // Returns: (status, body string)
    def send[A: Show](request: Request[IO]): IO[(Int, String)] =
      client.run(request).use { response =>
        val code = response.status.code
        response.as[String].map(code -> _)
      }

  }
}
