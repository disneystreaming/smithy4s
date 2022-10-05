package smithy4s.xml
package internals

import smithy4s.xml.XmlDocument
import smithy4s.xml.internals.XmlCursor.SingleNode
import smithy4s.xml.internals.XmlCursor.NoNode
import smithy4s.xml.internals.XmlCursor.FailedNode
import smithy4s.xml.internals.XmlCursor.AttrNode
import smithy4s.ConstraintError

case class XmlReadError(path: XPath, message: String) extends Throwable {
  override def getMessage(): String = s"${path.render}: $message"
}

trait XmlDecoder[A] { self =>
  def read(cursor: XmlCursor): Either[XmlReadError, A]
  final def map[B](f: A => B): XmlDecoder[B] = new XmlDecoder[B] {
    def read(cursor: XmlCursor): Either[XmlReadError, B] =
      self.read(cursor).map(f)
  }
  final def emap[B](f: A => Either[ConstraintError, B]): XmlDecoder[B] =
    new XmlDecoder[B] {
      def read(cursor: XmlCursor): Either[XmlReadError, B] =
        self.read(cursor).flatMap {
          f(_) match {
            case Left(e)      => Left(XmlReadError(cursor.history, e.message))
            case Right(value) => Right(value)
          }
        }
    }
  final def down(tag: String): XmlDecoder[A] = new XmlDecoder[A] {
    def read(cursor: XmlCursor): Either[XmlReadError, A] =
      self.read(cursor.down(tag))
  }
  final def attribute(attr: String): XmlDecoder[A] = new XmlDecoder[A] {
    def read(cursor: XmlCursor): Either[XmlReadError, A] =
      self.read(cursor.attr(attr))
  }
  final def optional: XmlDecoder[Option[A]] = new XmlDecoder[Option[A]] {
    def read(cursor: XmlCursor): Either[XmlReadError, Option[A]] = {
      cursor match {
        case NoNode(_) => Right(None)
        case other     => self.read(other).map(Some(_))
      }
    }
  }
}

object XmlDecoder {

  def alwaysFailing[A](message: String): XmlDecoder[A] = new XmlDecoder[A] {
    def read(cursor: XmlCursor): Either[XmlReadError, A] = Left(
      XmlReadError(cursor.history, message)
    )
  }

  def fromStringParser[A](expectedType: String)(
      f: String => Option[A]
  ): XmlDecoder[A] =
    new XmlDecoder[A] {
      def read(cursor: XmlCursor): Either[XmlReadError, A] = cursor match {
        case SingleNode(history, node) =>
          node.children match {
            case XmlDocument.XmlText(value) :: Nil =>
              f(value).toRight(
                XmlReadError(
                  history,
                  s"Could not extract $expectedType from $value"
                )
              )
            case _ =>
              Left(
                XmlReadError(
                  history,
                  s"Expected a single node with text content"
                )
              )
          }
        case AttrNode(history, values) =>
          if (values.tail.nonEmpty) {
            Left(XmlReadError(history, s"Expected a single text attribute"))
          } else {
            val value = values.head
            f(value).toRight(
              XmlReadError(
                history,
                s"Could not extract $expectedType from $value"
              )
            )
          }
        case FailedNode(history) =>
          Left(XmlReadError(history, s"Could not decode failed node"))
        case other =>
          Left(
            XmlReadError(
              other.history,
              s"Expected a single node with text content"
            )
          )
      }
    }

}
