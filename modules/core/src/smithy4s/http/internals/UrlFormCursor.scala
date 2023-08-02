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
import smithy4s.http.UrlForm

/**
  * This construct is an internal implementation-detail used for decoding
  * UrlForm payloads.
  *
  * It follows the model popularised by Argonaut and Circe, where "cursors" are
  * used instead of the direct data. This makes it easier to express the
  * decoding logic that needs to "peek further down" the UrlForm data.
  */
private[smithy4s] final case class UrlFormCursor(
    history: PayloadPath,
    values: List[UrlForm.FormData]
) {

  def down(segment: PayloadPath.Segment): UrlFormCursor =
    UrlFormCursor(
      history.append(segment),
      values.collect {
        case UrlForm
              .FormData(PayloadPath(`segment` :: segments), Some(value)) =>
          UrlForm.FormData(PayloadPath(segments), Some(value))
      }
    )

}
