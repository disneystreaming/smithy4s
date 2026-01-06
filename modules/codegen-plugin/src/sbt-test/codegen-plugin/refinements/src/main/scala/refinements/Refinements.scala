/*
 *  Copyright 2021-2026 Disney Streaming
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

package refinements.validated

import scripted.date.DateFormat

import java.time.LocalDate
import scala.util.Try
import smithy4s.{Refinement, RefinementProvider}


object Refinements {
  object dateFormat {
    private def newDate(value: String): Either[String, LocalDate] =
      Try(LocalDate.parse(value)).toEither.left.map(_.getMessage)

    object provider {
      implicit val forDate: RefinementProvider[DateFormat, String, LocalDate] =
        Refinement.drivenBy[DateFormat](newDate, _.toString)
    }
  }
}
