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

  // build a map: status code to alternative
  // exclude all status code that are used on multiple alternative
  // in essence, it gives a `Map[Int, SchemaAlt[E, _]]` that's used
  // for the lookup
  private val byStatusCode: Int => Option[SchemaAlt[E, _]] = {
    val perStatusCode: Map[Int, SchemaAlt[E, _]] = alts
      .flatMap { alt =>
        alt.hints.get(HttpError).map { he => he.value -> alt }
      }
      .groupBy(_._1)
      .collect {
        // Discard alternative where another alternative has the same http status code
        case (status, allAlts) if allAlts.size == 1 => status -> allAlts.head._2
      }
      .toMap
    val errorForStatus: Int => Option[SchemaAlt[E, _]] = perStatusCode.get

    lazy val fallbackError: Int => Option[SchemaAlt[E, _]] = {
      // grab the alt that's annotated with the expected `Error` hint
      // only if there is only one
      def forErrorType(expected: Error): Option[SchemaAlt[E, _]] = {
        val matchingAlts = alts
          .flatMap { alt =>
            alt.hints
              .get(HttpError)
              .fold(
                alt.hints.get(Error).collect {
                  case e if e == expected => alt
                }
              )(_ => None)

          }
        if (matchingAlts.size == 1) matchingAlts.headOption else None
      }
      val clientAlt: Option[SchemaAlt[E, _]] = forErrorType(Error.CLIENT)
      val serverAlt: Option[SchemaAlt[E, _]] = forErrorType(Error.SERVER)

      { intStatus =>
        if (intStatus >= 400 && intStatus < 500) clientAlt
        else if (intStatus >= 500 && intStatus < 600) serverAlt
        else None
      }
    }

    inputStatus =>
      errorForStatus(inputStatus).orElse(fallbackError(inputStatus))
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
