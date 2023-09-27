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
import cats.data.OptionT
import cats.effect.Sync
import org.http4s.HttpRoutes
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Host
import org.http4s.headers.Location
import org.webjars.WebJarAssetLocator

private[smithy4s] abstract class Docs[F[_]](
    ids: NonEmptyList[HasId],
    path: String,
    swaggerUiPath: String
)(implicit F: Sync[F])
    extends Http4sDsl[F] {

  def staticResource(
      name: String,
      req: Option[Request[F]]
  ): OptionT[F, Response[F]]

  private val specsPath = "specs"

  private def toSwaggerUrl(id: HasId): (String, SwaggerInit.SwaggerUrl) = {
    val jsonSpec = id.id.namespace + '.' + id.id.name + ".json"
    jsonSpec -> SwaggerInit.SwaggerUrl(s"./$specsPath/$jsonSpec", id.id.name)
  }
  private val validSpecs = ids.map(toSwaggerUrl).map(_._1).toList
  private val specsUrls = ids.map(toSwaggerUrl).map(_._2)

  private val actualPath: Path = Uri.Path.unsafeFromString("/" + path)

  object DocPath {
    def unapply(p: Path): Boolean = {
      p match {
        case `actualPath`      => true
        case `actualPath` / "" => true
        case _                 => false
      }
    }
  }

  private def fromReqToReclaimedUri(req: Request[F]): Option[Uri] = {
    for {
      isSecure <- req.isSecure
      auth <- req.uri.authority.orElse(
        req.headers
          .get[Host]
          .map(h =>
            Uri.Authority(None, host = Uri.RegName(h.host), port = h.port)
          )
      )
    } yield {
      val scheme =
        if (isSecure) Uri.Scheme.https
        else Uri.Scheme.http
      req.uri.copy(authority = Some(auth), scheme = Some(scheme))
    }
  }

  def routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case r @ GET -> DocPath() if r.uri.query.isEmpty =>
      val finalUri = fromReqToReclaimedUri(r) match {
        case None      => Uri(path = actualPath / "index.html")
        case Some(uri) => uri.withPath(uri.path / "index.html")
      }
      Found(Location(finalUri))

    case request @ GET -> `actualPath` / `specsPath` / jsonSpec
        if validSpecs.contains(jsonSpec) =>
      staticResource(jsonSpec, Some(request))
        .getOrElseF(InternalServerError())

    case GET -> `actualPath` / "swagger-initializer.js" =>
      SwaggerInit.asResponse[F](specsUrls)

    case request @ GET -> `actualPath` / filePath =>
      val resource = s"$swaggerUiPath/$filePath"
      staticResource(resource, Some(request)).getOrElseF(NotFound())
  }

}

private[smithy4s] trait SwaggerUiInit {
  private[this] lazy val swaggerUiVersion: String =
    new WebJarAssetLocator().getWebJars.get("swagger-ui-dist")

  protected lazy val swaggerUiResourcePath =
    s"META-INF/resources/webjars/swagger-ui-dist/$swaggerUiVersion"
}
