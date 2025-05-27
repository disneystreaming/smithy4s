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

import scala.runtime.AbstractFunction4

/**
 * RFC 3986 compliant URI implementation.
 * @param origin The origin component of the URI.
 * @param path A sequence of URL-decoded URI path segments
 * @param queryParams A map of query parameters where keys and values are URL-decoded
 * @param pathParams Optional map of path parameters extracted during routing
 */
final case class HttpUri(
    origin: Option[HttpUriOrigin],
    /**
      * A sequence of URL-decoded URI segment.
      */
    path: IndexedSeq[String],
    queryParams: Map[String, Seq[String]],
    /**
      * Field allowing to store decoded path parameters alongside an http request,
      * once the routing logic has come in effect.
      */
    pathParams: Option[Map[String, String]]
) {

  def scheme: Option[HttpUriScheme] = origin.flatMap(_.scheme)

  def authority: Option[HttpUriAuthority] = origin.map(_.authority)

  def host: Option[String] = origin.map(_.authority.host)

  def port: Option[Int] = origin.flatMap(_.authority.port)

  def userInfo: Option[String] = origin.flatMap(_.authority.userInfo)

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

  def withHost(host: String): HttpUri = {
    origin match {
      case Some(o) =>
        copy(origin = Some(o.copy(authority = o.authority.withHost(host))))
      case None =>
        copy(origin = Some(HttpUriOrigin.schemeRelative(host)))
    }

  }

}

object HttpUri
    extends AbstractFunction4[
      Option[HttpUriOrigin],
      IndexedSeq[String],
      Map[String, Seq[String]],
      Option[Map[String, String]],
      HttpUri
    ] {

  def apply(
      origin: Option[HttpUriOrigin],
      path: IndexedSeq[String],
      queryParams: Map[String, Seq[String]],
      pathParams: Option[Map[String, String]]
  ): HttpUri = {
    new HttpUri(origin, path, queryParams, pathParams)
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
