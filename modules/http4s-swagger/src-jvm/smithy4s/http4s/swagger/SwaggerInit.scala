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

import cats.Applicative
import cats.syntax.all._
import cats.data.NonEmptyList
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Type`
import scala.io.Source

object SwaggerInit {
  final case class SwaggerUrl(url: String, name: String)

  final val swaggerInitializerJs = {
    Source.fromResource("swagger-initializer.js").getLines().mkString("\n")
  }

  private abstract class response[F[_]: Applicative] extends Http4sDsl[F] {
    def build(urls: NonEmptyList[SwaggerUrl]): F[Response[F]] = {
      val content = javascript(urls)

      Ok(content).map(
        _.withHeaders(
          `Content-Type`.apply(MediaType.application.javascript)
        )
      )
    }
  }

  def asResponse[F[_]: Applicative](
      urls: NonEmptyList[SwaggerUrl]
  ): F[Response[F]] = {
    new response[F] {}.build(urls)
  }

  private def javascript(urls: NonEmptyList[SwaggerUrl]): String = {
    val finalUrls =
      urls.map(u => s""" {"url": "${u.url}", "name": "${u.name}"} """)
    val renderedUrls = finalUrls.toList.mkString(",")
    swaggerInitializerJs.replaceAll(
      "( *)url:.*,",
      s"$$1urls: [$renderedUrls],"
    )
  }
}
