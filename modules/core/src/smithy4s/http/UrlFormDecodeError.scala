/*
 *  Copyright 2021-2024 Disney Streaming
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

import smithy4s.codecs.PayloadPath
import smithy4s.http.internals.UrlFormCursor

final case class UrlFormDecodeError(
    path: PayloadPath,
    message: String
) extends Throwable {
  override def getMessage(): String = s"${path.render()}: $message"
}

private[http] object UrlFormDecodeError {

  def singleValueExpected(cursor: UrlFormCursor): UrlFormDecodeError =
    UrlFormDecodeError(
      cursor.history,
      s"Expected a single value but got ${cursor.values}"
    )

}
