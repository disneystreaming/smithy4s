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

package smithy4s.xml
package internals

import cats.data.NonEmptyList
import smithy4s.ConstraintError
import smithy4s.xml.XmlDocument
import smithy4s.xml.XmlDocument.XmlQName
import smithy4s.xml.internals.XmlCursor.AttrNode
import smithy4s.xml.internals.XmlCursor.FailedNode
import smithy4s.xml.internals.XmlCursor.NoNode
import smithy4s.xml.internals.XmlCursor.Nodes

/**
  * This constructs allow for decoding XML data. It is not limited to top-level
  * documents, and works against XmlCursor. Smithy4s does not have vocation to be
  * a general-purpose XML library, so we keep this as a private implementation detail
  * that should never be used directly.
  *
  * It exposes simple combinators that allow to indicate that the decoding should happen
  * further down the cursor, or enable the prevention of failure when decoding an absence
  * of node (which is the case for optional data).
  */
private[smithy4s] trait XmlDecoder[A] { self =>
  def decode(cursor: XmlCursor): Either[XmlDecodeError, A]
  final def map[B](f: A => B): XmlDecoder[B] = new XmlDecoder[B] {
    def decode(cursor: XmlCursor): Either[XmlDecodeError, B] =
      self.decode(cursor).map(f)
  }
  final def emap[B](f: A => Either[ConstraintError, B]): XmlDecoder[B] =
    new XmlDecoder[B] {
      def decode(cursor: XmlCursor): Either[XmlDecodeError, B] =
        self.decode(cursor).flatMap {
          f(_) match {
            case Left(e)      => Left(XmlDecodeError(cursor.history, e.message))
            case Right(value) => Right(value)
          }
        }
    }
  def down(tag: XmlQName): XmlDecoder[A] = new XmlDecoder[A] {
    def decode(cursor: XmlCursor): Either[XmlDecodeError, A] =
      self.decode(cursor.down(tag))
  }
  def attribute(attr: XmlQName): XmlDecoder[A] = new XmlDecoder[A] {
    def decode(cursor: XmlCursor): Either[XmlDecodeError, A] =
      self.decode(cursor.attr(attr))
  }
  def optional: XmlDecoder[Option[A]] = new XmlDecoder[Option[A]] {
    def decode(cursor: XmlCursor): Either[XmlDecodeError, Option[A]] = {
      cursor match {
        case NoNode(_) => Right(None)
        case other     => self.decode(other).map(Some(_))
      }
    }
  }
}

private[smithy4s] object XmlDecoder {

  def alwaysFailing[A](message: String): XmlDecoder[A] = new XmlDecoder[A] {
    def decode(cursor: XmlCursor): Either[XmlDecodeError, A] = Left(
      XmlDecodeError(cursor.history, message)
    )
  }

  /**
    * This is the method that is used to define primitive decoders, as all primitives
    * are decoded from text content, whether it's in elements or in attributes.
    */
  def fromStringParser[A](expectedType: String, trim: Boolean)(
      f: String => Option[A]
  ): XmlDecoder[A] =
    new XmlDecoder[A] {
      def decode(cursor: XmlCursor): Either[XmlDecodeError, A] = cursor match {
        case Nodes(history, NonEmptyList(node, Nil)) =>
          node.children match {
            case XmlDocument.XmlText(value) :: Nil =>
              f(if (trim) value.trim() else value).toRight(
                XmlDecodeError(
                  history,
                  s"Could not extract $expectedType from $value"
                )
              )
            case Nil =>
              f("").toRight(
                XmlDecodeError(
                  history,
                  s"Could not extract $expectedType from empty string"
                )
              )
            case _ =>
              Left(
                XmlDecodeError(
                  history,
                  s"Expected a single node with text content"
                )
              )
          }
        case AttrNode(history, values) =>
          if (values.tail.nonEmpty) {
            Left(XmlDecodeError(history, s"Expected a single text attribute"))
          } else {
            val value = values.head
            f(value).toRight(
              XmlDecodeError(
                history,
                s"Could not extract $expectedType from $value"
              )
            )
          }
        case FailedNode(history) =>
          Left(XmlDecodeError(history, s"Could not decode failed node"))
        case other =>
          Left(
            XmlDecodeError(
              other.history,
              s"Expected a single node with text content"
            )
          )
      }
    }

}
