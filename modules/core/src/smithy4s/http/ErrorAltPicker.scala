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
package http

import smithy.api.Error
import smithy.api.Error.CLIENT
import smithy.api.Error.SERVER
import smithy4s.schema.SchemaAlt

/**
  * Utility class to help find the best alternative out of a error union
  * type. This is useful when deserializing on the client side of a request/
  * response round trip.
  *
  * @param alts alternatives of the error union to choose from
  */
final class ErrorAltPicker[E](alts: Vector[SchemaAlt[E, _]]) {
  private lazy val withHints = alts.map(a => a -> a.instance.hints)
  private lazy val (generic, rest) = withHints.partition {
    case (_, Error.hint(_)) => true
    case (_, _)             => false
  }
  private lazy val clientErrors = generic.collect {
    case (a, Error.hint(CLIENT)) => a
  }
  private lazy val serverErrors = generic.collect {
    case (a, Error.hint(SERVER)) => a
  }
  private lazy val others = rest.collect {
    case (a, smithy.api.HttpError.hint(extracted)) =>
      a -> extracted.value
  }

  def orderedForStatus(status: Int): Vector[SchemaAlt[E, _]] = {
    val generics =
      if (status >= 400 && status < 500) {
        clientErrors ++ serverErrors
      } else {
        serverErrors ++ clientErrors
      }
    generics ++ others.map(_._1)
  }

  def getPreciseAlternative(status: Int): Option[SchemaAlt[E, _]] = {
    others
      .find(_._2 == status)
      .map(_._1)
      .orElse(
        if (status == 400 && clientErrors.size == 1)
          clientErrors.headOption
        else if (status == 500 && serverErrors.size == 1)
          serverErrors.headOption
        else None
      )
  }
}
