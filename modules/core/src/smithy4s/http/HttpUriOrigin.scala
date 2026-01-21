/*
 *  Copyright 2021-2026 Disney Streaming
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

/**
 * Represents a complete network location including scheme and authority components.
 * This enforces that a scheme can only be present when there is a host.
 * Based off of RFC 3986 and 6454 (UriOrigin).
 * @param scheme The URI scheme (e.g. http, https). Optional for relative URIs.
 * @param authority The authority component of the URI. Required if scheme is present.
 */
final case class HttpUriOrigin private (
    scheme: Option[HttpUriScheme],
    authority: HttpUriAuthority
) {

  /**
   * Renders the origin according to RFC 3986
   */
  def render: String = {
    val schemeStr = scheme
      .map {
        case HttpUriScheme.Http  => "http://"
        case HttpUriScheme.Https => "https://"
      }
      .getOrElse("//")
    s"$schemeStr${authority.render}"
  }

  def withHostPrefix(prefix: String): HttpUriOrigin =
    copy(authority = authority.hostPrefix(prefix))

  def withAuthority(authority: HttpUriAuthority): HttpUriOrigin =
    copy(authority = authority)

  /**
   * Creates a new HttpUriOrigin with the given scheme
   */
  def withScheme(scheme: HttpUriScheme): HttpUriOrigin =
    copy(scheme = Some(scheme))

  /**
   * Creates a new HttpUriOrigin with the given port
   */
  def withPort(port: Int): HttpUriOrigin =
    copy(authority = authority.withPort(port))

  /**
   * Creates a new HttpUriOrigin with the given user info
   */
  def withUserInfo(userInfo: String): HttpUriOrigin =
    copy(authority = authority.withUserInfo(userInfo))

  /**
   * Creates a new HttpUriOrigin without scheme
   */
  def withoutScheme: HttpUriOrigin = copy(scheme = None)

  /**
   * Creates a new HttpUriOrigin without port
   */
  def withoutPort: HttpUriOrigin = copy(authority = authority.withoutPort)

  /**
   * Creates a new HttpUriOrigin without user info
   */
  def withoutUserInfo: HttpUriOrigin =
    copy(authority = authority.withoutUserInfo)
}

object HttpUriOrigin {

  @scala.annotation.nowarn(
    "msg=private method unapply in object HttpUriOrigin is never used"
  )
  private def unapply(
      origin: HttpUriOrigin
  ): Option[(Option[HttpUriScheme], HttpUriAuthority)] = {
    Some((origin.scheme, origin.authority))
  }

  def apply(
      scheme: Option[HttpUriScheme],
      authority: HttpUriAuthority
  ): HttpUriOrigin = new HttpUriOrigin(scheme, authority)

  /**
   * Creates a scheme-relative origin (starts with //)
   */
  def schemeRelative(host: String, port: Option[Int] = None): HttpUriOrigin =
    new HttpUriOrigin(None, HttpUriAuthority(host, port))

  /**
   * Creates an absolute origin with scheme
   */
  def absolute(
      scheme: HttpUriScheme,
      host: String,
      port: Option[Int] = None
  ): HttpUriOrigin =
    new HttpUriOrigin(Some(scheme), HttpUriAuthority(host, port))

  /**
   * Creates an absolute origin with scheme, host, and port
   */
  def absolute(
      scheme: HttpUriScheme,
      host: String,
      port: Int,
      userInfo: String
  ): HttpUriOrigin =
    new HttpUriOrigin(
      Some(scheme),
      HttpUriAuthority(host, Some(port), Some(userInfo))
    )

}
