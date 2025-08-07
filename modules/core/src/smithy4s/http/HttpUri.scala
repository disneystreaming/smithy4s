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

package smithy4s.http
import java.net.URI

/**
 * RFC 3986 compliant URI implementation.
 * @param origin The origin component of the URI.
 * @param path A sequence of URL-decoded URI path segments
 * @param queryParams A map of query parameters where keys and values are URL-decoded
 * @param pathParams Optional map of path parameters extracted during routing
 */
final case class HttpUri private (
    origin: Option[HttpUriOrigin],
    path: IndexedSeq[String],
    queryParams: Map[String, Seq[String]],
    pathParams: Option[Map[String, String]]
) {
  def toURI: URI = {
    val schemeStr = scheme.map {
      case HttpUriScheme.Http  => "http"
      case HttpUriScheme.Https => "https"
    }
    val pathStr = path.mkString("/", "/", "")
    val queryStr = queryParams
      .map { case (k, v) =>
        v.map(vv => s"$k=$vv").mkString("&")
      }
      .mkString("&")
    new URI(
      schemeStr.getOrElse(null),
      null,
      host.getOrElse(null),
      port.getOrElse(-1),
      pathStr,
      queryStr,
      null
    )
  }

  def authority: Option[HttpUriAuthority] = origin.map(_.authority)

  def host: Option[String] = origin.map(_.authority.host)

  def port: Option[Int] = origin.flatMap(_.authority.port)

  def userInfo: Option[String] = origin.flatMap(_.authority.userInfo)

  def scheme: Option[HttpUriScheme] = origin.flatMap(_.scheme)

  /**
   * Returns true if this is a relative URI (no authority)
   */
  def isRelative: Boolean = origin.isEmpty

  /**
   * Returns true if this is an absolute URI (has scheme)
   */
  def isAbsolute: Boolean = origin.exists(_.scheme.isDefined)

  /**
   * Returns true if this is a scheme-relative URI (starts with //)
   */
  def isSchemeRelative: Boolean = origin.exists(_.scheme.isEmpty)

  def withOrigin(origin: HttpUriOrigin): HttpUri = {
    copy(origin = Some(origin))
  }

  def transformOrigin(
      f: HttpUriOrigin => HttpUriOrigin
  ): HttpUri = {
    origin match {
      case Some(o) => copy(origin = Some(f(o)))
      case None    => this
    }
  }

  def withHost(host: String): HttpUri = {
    origin match {
      case Some(o) =>
        copy(origin = Some(o.withAuthority(o.authority.withHost(host))))
      case None =>
        copy(origin = Some(HttpUriOrigin.schemeRelative(host)))
    }
  }

  def withHostPrefix(prefix: String): HttpUri = {
    origin match {
      case Some(o) =>
        copy(origin = Some(o.withHostPrefix(prefix)))
      case None => this
    }
  }
  def withPort(port: Int): HttpUri = {
    origin match {
      case Some(o) =>
        copy(origin = Some(o.withAuthority(o.authority.withPort(port))))
      case None => this
    }
  }

  def transformPath(
      f: IndexedSeq[String] => IndexedSeq[String]
  ): HttpUri = {
    copy(path = f(path))
  }
  def withPath(path: IndexedSeq[String]): HttpUri = {
    copy(path = path)
  }
  def withQueryParams(
      queryParams: Map[String, Seq[String]]
  ): HttpUri = {
    copy(queryParams = queryParams)
  }
  def withoutQueryParams: HttpUri = {
    copy(queryParams = Map.empty)
  }
  def transformQueryParams(
      f: Map[String, Seq[String]] => Map[String, Seq[String]]
  ): HttpUri = {
    copy(queryParams = f(queryParams))
  }
  def withPathParams(
      pathParams: Map[String, String]
  ): HttpUri = {
    copy(pathParams = Some(pathParams))
  }
  def withoutPathParams: HttpUri = {
    copy(pathParams = None)
  }
  def transformPathParams(
      f: Map[String, String] => Map[String, String]
  ): HttpUri = {
    pathParams match {
      case Some(params) => copy(pathParams = Some(f(params)))
      case None         => this
    }
  }

}

object HttpUri {

  @scala.annotation.nowarn(
    "msg=private method unapply in object HttpUri is never used"
  )
  private def unapply(
      uri: HttpUri
  ): Option[
    (
        Option[HttpUriOrigin],
        IndexedSeq[String],
        Map[String, Seq[String]],
        Option[Map[String, String]]
    )
  ] = {
    Some((uri.origin, uri.path, uri.queryParams, uri.pathParams))
  }

  def apply(
      origin: Option[HttpUriOrigin],
      path: IndexedSeq[String],
      queryParams: Map[String, Seq[String]],
      pathParams: Option[Map[String, String]]
  ): HttpUri = {
    new HttpUri(origin, path, queryParams, pathParams)
  }
  def fromURI(uri: URI): HttpUri = {
    val scheme = Option(uri.getScheme()).map {
      case "https" => HttpUriScheme.Https
      case _       => HttpUriScheme.Http
    }
    val host = Option(uri.getHost())
    val port = Option(uri.getPort()).filter(_ >= 0)
    val path = uri.getPath.split('/').filter(_.nonEmpty).toIndexedSeq
    val queryParams: Map[String, Seq[String]] = Option(uri.getQuery())
      .map { query =>
        query
          .split("&")
          .map { pair =>
            pair.split("=") match {
              case Array(k: String, v: String) => k -> Seq(v)
              case Array(k: String)            => k -> Seq.empty[String]
              // cases where you have q1=v1=v2 => q1 -> "v1=v2"
              case v @ Array(k: String, _*) => k -> Seq(v.tail.mkString("="))
            }
          }
          .groupBy(_._1)
          .map { case (k, vs) => k -> vs.map(_._2).flatten.toSeq }
      }
      .getOrElse(Map.empty)
    val origin = host.map(hn => {
      HttpUriOrigin(
        scheme = scheme,
        authority = HttpUriAuthority(hn, port)
      )
    })
    HttpUri(
      origin = origin,
      path = path,
      queryParams = queryParams,
      pathParams = None
    )
  }

  /**
   * Creates a relative URI with path and query parameters
   */
  def relative(
      path: IndexedSeq[String],
      queryParams: Map[String, Seq[String]],
      pathParams: Option[Map[String, String]] = None
  ): HttpUri = {
    HttpUri(
      origin = None,
      path = path,
      queryParams = queryParams,
      pathParams = pathParams
    )
  }

  /**
   * Creates a scheme-relative URI (starts with //)
   */
  def schemeRelative(
      host: String,
      port: Option[Int],
      path: IndexedSeq[String],
      queryParams: Map[String, Seq[String]] = Map.empty,
      pathParams: Option[Map[String, String]] = None
  ): HttpUri = {
    HttpUri(
      origin = Some(HttpUriOrigin.schemeRelative(host, port)),
      path = path,
      queryParams = queryParams,
      pathParams = pathParams
    )
  }

  /**
   * Creates an absolute URI
   */
  def absolute(
      scheme: HttpUriScheme,
      host: String,
      port: Option[Int],
      path: IndexedSeq[String],
      queryParams: Map[String, Seq[String]] = Map.empty,
      pathParams: Option[Map[String, String]] = None
  ): HttpUri = {

    HttpUri(
      origin = Some(HttpUriOrigin.absolute(scheme, host, port)),
      path = path,
      queryParams = queryParams,
      pathParams = pathParams
    )
  }
}
