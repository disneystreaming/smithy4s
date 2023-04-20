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

package smithy4s.http4s.swagger

import cats.effect.IO
import org.http4s._
import org.http4s.implicits._
import org.typelevel.ci.CIString
import weaver._
import smithy4s.HasId
import smithy4s.ShapeId

object DocsSpec extends SimpleIOSuite {

  def mkDocs = smithy4s.http4s.swagger.docs[IO]

  def service = new HasId {
    def id: ShapeId = ShapeId("foobar", "test-spec")
  }

  def docs(path: String) =
    mkDocs
      .withPath(path)
      .withSwaggerUiResources("swaggerui")(service)

  List("docs", "example/docs", "very/long/example/docs").foreach { path =>
    val app = docs(path).orNotFound

    test(s"GET /$path redirects to expected location") {
      val request =
        Request[IO](
          method = Method.GET,
          uri = Uri.unsafeFromString(s"/$path")
        )
      app.run(request).map { response =>
        val redirectUri = response.headers
          .get(CIString("Location"))
          .map(_.head)
          .map(_.value)

        expect(response.status == Status.Found) and
          expect.eql(
            redirectUri,
            Some(s"/$path/index.html")
          )
      }
    }
    test(s"GET /$path/ redirects to expected location") {
      val request =
        Request[IO](method = Method.GET, uri = Uri.unsafeFromString(s"/$path"))
      app.run(request).map { response =>
        val redirectUri = response.headers
          .get(CIString("Location"))
          .map(_.head)
          .map(_.value)

        expect(response.status == Status.Found) and
          expect.eql(
            redirectUri,
            Some(s"/$path/index.html")
          )
      }
    }

    test(s"GET $path/test-file.json fetches requested file") {
      val filePath = s"/$path/test-file.json"
      val request =
        Request[IO](method = Method.GET, uri = Uri.unsafeFromString(filePath))
      app.run(request).map { response =>
        expect(response.status == Status.Ok)
      }
    }

    test(s"GET $path/specs/test-spec.json fetches service spec") {
      val filePath = "/foobar.test-spec.json"
      val request =
        Request[IO](
          method = Method.GET,
          uri = Uri.unsafeFromString(s"/$path/specs$filePath")
        )
      app.run(request).map { response =>
        expect(response.status == Status.Ok)
      }
    }
  }

  test(s"GET /irrelevant returns 404") {
    val filePath = s"/irrelevant"
    val request =
      Request[IO](method = Method.GET, uri = Uri.unsafeFromString(filePath))
    val app = docs("docs").orNotFound
    app.run(request).map { response =>
      expect(response.status == Status.NotFound)
    }
  }

  pureTest("Default swagger-ui-path") {
    val docs = mkDocs
    expect(
      docs.swaggerUiPath.startsWith(
        "META-INF/resources/webjars/swagger-ui-dist/"
      )
    )
  }

  test("redirect works correctly with an empty path") {
    val request =
      Request[IO](
        method = Method.GET,
        uri = Uri.unsafeFromString("/")
      )
    val app = docs("").orNotFound
    app.run(request).map { response =>
      val redirectUri = response.headers
        .get(CIString("Location"))
        .map(_.head)
        .map(_.value)

      expect(response.status == Status.Found) and
        expect.eql(
          redirectUri,
          Some(s"/index.html")
        )
    }
  }
}
