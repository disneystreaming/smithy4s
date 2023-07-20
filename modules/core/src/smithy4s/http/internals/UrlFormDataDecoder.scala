/*
 *  Copyright 2023 Disney Streaming
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

package smithy4s
package http
package internals

import smithy4s.codecs.PayloadPath

private[smithy4s] trait UrlFormDataDecoder[A] { self =>
  def decode(cursor: UrlFormCursor): Either[UrlFormDecodeError, A]
  final def map[B](f: A => B): UrlFormDataDecoder[B] =
    new UrlFormDataDecoder[B] {
      def decode(cursor: UrlFormCursor): Either[UrlFormDecodeError, B] =
        self.decode(cursor).map(f)
    }

  final def emap[B](f: A => Either[ConstraintError, B]): UrlFormDataDecoder[B] =
    new UrlFormDataDecoder[B] {
      def decode(cursor: UrlFormCursor): Either[UrlFormDecodeError, B] =
        self.decode(cursor).flatMap {
          f(_) match {
            case Left(e) => Left(UrlFormDecodeError(cursor.history, e.message))
            case Right(value) => Right(value)
          }
        }
    }
  def down(segment: PayloadPath.Segment): UrlFormDataDecoder[A] =
    new UrlFormDataDecoder[A] {
      def decode(cursor: UrlFormCursor): Either[UrlFormDecodeError, A] =
        self.decode(cursor.down(segment))
    }

  def optional: UrlFormDataDecoder[Option[A]] =
    new UrlFormDataDecoder[Option[A]] {
      def decode(
          cursor: UrlFormCursor
      ): Either[UrlFormDecodeError, Option[A]] = {
        cursor match {
          case UrlFormCursor.Empty(_) => Right(None)
          case other                  => self.decode(other).map(Some(_))
        }
      }
    }
}

private[smithy4s] object UrlFormDataDecoder {

  def alwaysFailing[A](message: String): UrlFormDataDecoder[A] =
    new UrlFormDataDecoder[A] {
      def decode(cursor: UrlFormCursor): Either[UrlFormDecodeError, A] = Left(
        UrlFormDecodeError(cursor.history, message)
      )
    }

  def fromStringParser[A](expectedType: String)(
      f: String => Option[A]
  ): UrlFormDataDecoder[A] =
    new UrlFormDataDecoder[A] {
      def decode(cursor: UrlFormCursor): Either[UrlFormDecodeError, A] =
        cursor match {
          // TODO: Flatten FormData types
          case UrlFormCursor.Value(
                history,
                UrlForm.FormData.MultipleValues(
                  // TODO: Change values to list
                  Vector(pathedValue)
                )
              ) =>
            pathedValue match {
              // TODO: Verify the path?
              case UrlForm.FormData.PathedValue(_, value) =>
                f(value).toRight(
                  UrlFormDecodeError(
                    history,
                    s"Could not extract $expectedType from $value"
                  )
                )
            }
          case UrlFormCursor.FailedValue(history) =>
            Left(UrlFormDecodeError(history, s"Could not decode failed value"))
          case other =>
            Left(
              UrlFormDecodeError(
                other.history,
                s"Expected a single value"
              )
            )
        }
    }

}
