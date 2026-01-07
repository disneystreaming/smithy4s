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

final case class HttpUriAuthority private (
    host: String,
    port: Option[Int],
    userInfo: Option[String]
) {
  def render: String = {
    val userInfoStr = userInfo.map(ui => s"$ui@").getOrElse("")
    val portStr = port.map(p => s":$p").getOrElse("")
    s"$userInfoStr$host$portStr"
  }

  def hostPrefix(prefix: String): HttpUriAuthority =
    copy(host = s"$prefix$host")

  def withHost(host: String): HttpUriAuthority = copy(host = host)
  def withPort(port: Int): HttpUriAuthority = copy(port = Some(port))
  def withUserInfo(userInfo: String): HttpUriAuthority =
    copy(userInfo = Some(userInfo))
  def withoutUserInfo: HttpUriAuthority = copy(userInfo = None)
  def withoutPort: HttpUriAuthority = copy(port = None)

}
object HttpUriAuthority {
  @scala.annotation.nowarn(
    "msg=private method unapply in object HttpUriAuthority is never used"
  )
  private def unapply(
      authority: HttpUriAuthority
  ): Option[(String, Option[Int], Option[String])] = {
    Some((authority.host, authority.port, authority.userInfo))

  }
  def apply(
      host: String,
      port: Option[Int] = None,
      userInfo: Option[String] = None
  ): HttpUriAuthority = new HttpUriAuthority(host, port, userInfo)
}
