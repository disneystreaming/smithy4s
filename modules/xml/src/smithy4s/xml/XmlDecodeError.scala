package smithy4s.xml

final case class XmlDecodeError(path: XPath, message: String)
    extends Throwable {
  override def getMessage(): String = s"${path.render}: $message"
}
