package smithy4s
package aws.query

import smithy4s.PayloadPath
import smithy4s.http.internals.URIEncoderDecoder

private[aws] sealed trait FormData extends Product with Serializable {
  def render: String
  def prepend(key: String): FormData
  def prepend(index: Int): FormData
}
private[aws] object FormData {
  final case object Empty extends FormData {
    override def render: String = ""

    override def prepend(key: String): FormData = this

    override def prepend(index: Int): FormData = this
  }

  final case class SimpleValue(str: String) extends FormData {
    override def render: String = URIEncoderDecoder.encode(str)

    override def prepend(key: String): FormData = {
      PathedValue(PayloadPath.fromString(key), str)
    }

    override def prepend(index: Int): FormData =
      PathedValue(PayloadPath.fromString(index.toString), str)
  }
  final case class PathedValue(path: PayloadPath, value: String)
      extends FormData {

    /**
     * @todo Understand the root reason and have a better solution for a workaround removing the '.' prefix.
     */
    override def render: String =
      URIEncoderDecoder.encode(path.toString.stripPrefix(".")) + "=" +
        URIEncoderDecoder.encode(value)

    override def prepend(key: String): FormData =
      copy(path.prepend(PayloadPath.Segment(key)), value)

    override def prepend(index: Int): FormData =
      copy(path.prepend(PayloadPath.Segment(index)), value)
  }
  final case class MultipleValues(values: Vector[FormData]) extends FormData {
    override def render: String =
      values.map(_.render).filter(str => str.nonEmpty).mkString("&")

    override def prepend(key: String): FormData =
      copy(values.map(_.prepend(key)))

    override def prepend(index: Int): FormData =
      copy(values.map(_.prepend(index)))
  }
}
