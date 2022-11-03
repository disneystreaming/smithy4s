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

import smithy.api.Error
import smithy4s.schema.SchemaAlt
import ErrorAltPicker.ErrorDiscriminator
import smithy.api.HttpError
import smithy4s.http.ErrorAltPicker.ErrorDiscriminator.FullId
import smithy4s.http.ErrorAltPicker.ErrorDiscriminator.NameOnly
import smithy4s.http.ErrorAltPicker.ErrorDiscriminator.StatusCode

/**
  * Utility class to help find the best alternative out of a error union
  * type. This is useful when deserializing on the client side of a request/
  * response round trip.
  *
  * @param alts alternatives of the error union to choose from
  */
final class ErrorAltPicker[E](alts: Vector[SchemaAlt[E, _]]) {
  private val byShapeId = alts
    .map { alt => alt.instance.shapeId -> alt }
    .toMap[ShapeId, SchemaAlt[E, _]]

  private val byName = alts
    .map(alt => alt.instance.shapeId.name -> alt)
    .toMap[String, SchemaAlt[E, _]]

  private val byStatusCode: Int => Option[SchemaAlt[E, _]] = { inputStatus =>
    val errorForStatus: Option[SchemaAlt[E, _]] = {
      val results =
        alts.filter(_.hints.get(HttpError).exists(_.value == inputStatus))
      if (results.length == 1) results.headOption else None
    }
    lazy val fallbackError: Option[SchemaAlt[E, _]] = {
      val matchingAlts = alts.filter { alt =>
        alt.hints.get(Error) match {
          case Some(e) if alt.hints.get(HttpError).isEmpty =>
            // If Error trait is present (must be 'client' or 'server') and
            // the status code is in the client/server error range, then use that alt
            val isClientError =
              e.value.toLowerCase == "client" && inputStatus >= 400 && inputStatus < 500
            val isServerError =
              e.value.toLowerCase == "server" && inputStatus >= 500 && inputStatus < 600
            isClientError || isServerError
          case _ => false
        }
      }
      if (matchingAlts.size == 1) matchingAlts.headOption else None
    }
    errorForStatus.orElse(fallbackError)
  }

  def getPreciseAlternative(
      discriminator: ErrorDiscriminator
  ): Option[SchemaAlt[E, _]] = {
    discriminator match {
      case FullId(shapeId) => byShapeId.get(shapeId)
      case NameOnly(name)  => byName.get(name)
      case StatusCode(int) => byStatusCode(int)
    }
  }
}

object ErrorAltPicker {
  sealed trait ErrorDiscriminator extends Product with Serializable

  object ErrorDiscriminator {
    case class FullId(shapeId: ShapeId) extends ErrorDiscriminator
    case class NameOnly(name: String) extends ErrorDiscriminator
    case class StatusCode(int: Int) extends ErrorDiscriminator
  }
}
