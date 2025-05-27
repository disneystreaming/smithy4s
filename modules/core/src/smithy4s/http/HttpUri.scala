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
import scala.runtime.AbstractFunction6

/**
 * Represents an HTTP URI.
 *
 * @param path A sequence of URL-decoded URI segments.
 * @param pathParams Field allowing to store decoded path parameters alongside an http request, once the routing logic has come in effect.
 */
final case class HttpUri(
    scheme: HttpUriScheme,
    host: String,
    port: Option[Int],
    path: IndexedSeq[String],
    queryParams: Map[String, Seq[String]],
    pathParams: Option[Map[String, String]]
) {
  def toURI: URI = {
    val schemeStr = scheme match {
      case HttpUriScheme.Http  => "http"
      case HttpUriScheme.Https => "https"
    }

    val pathStr = path.mkString("/", "/", "")
    val queryStr = queryParams
      .map { case (k, v) =>
        v.map(vv => s"$k=$vv").mkString("&")
      }
      .mkString("&")
    new URI(schemeStr, null, host, port.getOrElse(-1), pathStr, queryStr, null)
  }
}

object HttpUri
    extends AbstractFunction6[
      HttpUriScheme,
      String,
      Option[Int],
      IndexedSeq[String],
      Map[String, Seq[String]],
      Option[Map[String, String]],
      HttpUri
    ] {
  def apply(
      scheme: HttpUriScheme,
      host: String,
      port: Option[Int],
      path: IndexedSeq[String],
      queryParams: Map[String, Seq[String]],
      pathParams: Option[Map[String, String]]
  ): HttpUri = new HttpUri(scheme, host, port, path, queryParams, pathParams)

  def fromURI(uri: URI): HttpUri = {
    val scheme = uri.getScheme() match {
      case "https" => HttpUriScheme.Https
      case _       => HttpUriScheme.Http
    }
    val host = uri.getHost()
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
    HttpUri(scheme, host, port, path, queryParams, None)
  }
}
