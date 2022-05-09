/*
 *  Copyright 2021 Disney Streaming
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

import smithy4s._

import scala.scalajs.js
import scala.util.control.NonFatal

object JsConverters {

  private[this] def convertAnyToDocumentUnsafe(input: Any): Document =
    input match {
      case s: String => Document.fromString(s)
      case n: Double => Document.fromDouble(n)
      case true      => Document.fromBoolean(true)
      case false     => Document.fromBoolean(false)
      case null      => Document.DNull
      case a: js.Array[_] =>
        Document.DArray(a.map(convertAnyToDocumentUnsafe(_: Any)).toVector)
      case o: js.Object =>
        Document.DObject(
          o.asInstanceOf[js.Dictionary[_]]
            .view
            .mapValues(convertAnyToDocumentUnsafe)
            .toMap
        )
      case other if js.isUndefined(other) => Document.DNull
    }

  /**
   * Convert [[scala.scalajs.js.Any]] to [[Document]].
   */
  def convertJsToDocument(input: js.Any): Either[Throwable, Document] =
    try Right(convertAnyToDocumentUnsafe(input))
    catch {
      case NonFatal(exception) => Left(exception)
    }

}
