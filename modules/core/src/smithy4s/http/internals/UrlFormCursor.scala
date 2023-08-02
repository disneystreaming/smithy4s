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
private[smithy4s] sealed trait UrlFormCursor {

  def history: PayloadPath

  def down(segment: PayloadPath.Segment): UrlFormCursor

}

private[smithy4s] object UrlFormCursor {

  def fromUrlForm(urlForm: UrlForm): UrlFormCursor =
    Value(PayloadPath.root, urlForm.formData)

  case class Value(
      override val history: PayloadPath,
      formData: UrlForm.FormData.KeyValues
  ) extends UrlFormCursor {
    override def down(segment: PayloadPath.Segment): UrlFormCursor = {
      val matchingValues = formData.values.collect {
        case keyValue
            if keyValue.path.segments.headOption.contains(segment) &&
              keyValue.maybeValue.isDefined =>
          UrlForm.FormData.KeyValue(
            PayloadPath(keyValue.path.segments.tail),
            keyValue.maybeValue
          )
      }
      if (matchingValues.nonEmpty)
        Value(
          history.append(segment),
          UrlForm.FormData.KeyValues(matchingValues)
        )
      else
        Empty(history.append(segment))
    }
  }

  case class Empty(override val history: PayloadPath) extends UrlFormCursor {
    override def down(segment: PayloadPath.Segment): UrlFormCursor =
      Empty(
        history.append(segment)
      )
  }
}
