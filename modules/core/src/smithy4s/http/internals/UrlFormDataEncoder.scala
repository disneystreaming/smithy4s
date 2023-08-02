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

package smithy4s
package http
package internals

import smithy4s.capability.EncoderK
import smithy4s.codecs.PayloadPath
import smithy4s.http.UrlForm

private[smithy4s] trait UrlFormDataEncoder[-A] { self =>

  def encode(value: A): List[UrlForm.FormData]

  def contramap[B](f: B => A): UrlFormDataEncoder[B] =
    value => self.encode(f(value))

  def prepend(segment: PayloadPath.Segment): UrlFormDataEncoder[A] =
    value => self.encode(value).map(_.prepend(segment))

}

object UrlFormDataEncoder {

  implicit val urlFormDataEncoderK
      : EncoderK[UrlFormDataEncoder, List[UrlForm.FormData]] =
    new EncoderK[UrlFormDataEncoder, List[UrlForm.FormData]] {

      override def absorb[A](
          f: A => List[UrlForm.FormData]
      ): UrlFormDataEncoder[A] =
        f(_)

      override def apply[A](
          fa: UrlFormDataEncoder[A],
          a: A
      ): List[UrlForm.FormData] =
        fa.encode(a)

    }
}
