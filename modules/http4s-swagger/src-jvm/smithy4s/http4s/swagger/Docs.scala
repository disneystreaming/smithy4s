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

package smithy4s
package http4s
package swagger

import cats.data.NonEmptyList
import cats.effect.Sync
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Location
import org.http4s.HttpRoutes
import org.webjars.WebJarAssetLocator

private[smithy4s] abstract class Docs[F[_]](
    hasId: HasId,
    path: String,
    swaggerUiPath: String
)(implicit F: Sync[F])
    extends Http4sDsl[F]
    with Compat.DocsClass[F] {

  val jsonSpec = hasId.id.namespace + '.' + hasId.id.name + ".json"

  val actualPath: Path = Uri.Path.unsafeFromString("/" + path)

  object DocPath {
    def unapply(p: Path): Boolean = {
      p match {
        case `actualPath`      => true
        case `actualPath` / "" => true
        case _                 => false
      }
    }
  }
  def routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case r @ GET -> DocPath() if r.uri.query.isEmpty =>
      Found(Location(Uri.unsafeFromString(s"/$path/index.html")))

    case GET -> `actualPath` / "swagger-initializer.js" =>
      SwaggerInit.asResponse[F](
        NonEmptyList(
          SwaggerInit.SwaggerUrl(s"/$jsonSpec", hasId.id.name),
          Nil
        )
      )

    case request @ GET -> `actualPath` / filePath =>
      val resource = s"$swaggerUiPath/$filePath"
      staticResource(resource, Some(request)).getOrElseF(NotFound())

    case request @ GET -> Root / `jsonSpec` =>
      staticResource(jsonSpec, Some(request))
        .getOrElseF(InternalServerError())
  }
}

object Docs extends Compat.DocsCompanion {}

trait SwaggerUiInit {
  private[this] lazy val swaggerUiVersion: String =
    new WebJarAssetLocator().getWebJars.get("swagger-ui-dist")

  protected lazy val swaggerUiPath =
    s"META-INF/resources/webjars/swagger-ui-dist/$swaggerUiVersion"
}
