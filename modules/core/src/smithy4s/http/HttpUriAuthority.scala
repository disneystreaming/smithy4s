package smithy4s.http

final case class HttpUriAuthority(
    host: String,
    port: Option[Int] = None,
    userInfo: Option[String] = None
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
