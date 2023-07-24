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

  def contramap[B](f: B => A): UrlFormDataEncoder[B] =
    (value: B) => self.encode(f(value))

  def encode(value: A): UrlForm.FormData

  def prepend(segment: PayloadPath.Segment): UrlFormDataEncoder[A] =
    (value: A) => self.encode(value).prepend(segment)

}
