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

import smithy4s.http.UrlForm
import smithy4s.codecs.PayloadPath

private[smithy4s] trait UrlFormDataEncoder[-A] { self =>
  def encode(value: A): UrlForm.FormData

  def contramap[B](f: B => A): UrlFormDataEncoder[B] =
    new UrlFormDataEncoder[B] {
      def encode(value: B): UrlForm.FormData = self.encode(f(value))
      override def optional: UrlFormDataEncoder[Option[B]] =
        self.optional.contramap[Option[B]](_.map(f))
    }

  def prepend(segment: PayloadPath.Segment): UrlFormDataEncoder[A] =
    new UrlFormDataEncoder[A] {
      def encode(value: A): UrlForm.FormData =
        self.encode(value).prepend(segment)
    }

  def optional: UrlFormDataEncoder[Option[A]] = {
    (_: Option[A]) match {
      case None    => UrlForm.FormData.Empty
      case Some(a) => self.encode(a)
    }
  }

}

object UrlFormDataEncoder {
  // TODO: What's this for?
  val empty: UrlFormDataEncoder[Any] = { (_: Any) => UrlForm.FormData.Empty }
}
