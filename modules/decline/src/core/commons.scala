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

package smithy4s.decline.core

import cats.Functor
import cats.MonadError
import cats.data.Validated.Valid
import cats.implicits._
import com.monovore.decline.Argument
import smithy4s.Blob
import smithy4s.ConstraintError
import smithy4s.Document
import smithy4s.Schema
import smithy4s.capability.Covariant
import smithy4s.time._

import java.time.format.DateTimeFormatter
import java.util.Base64
import scala.concurrent.duration.Duration
import scala.util.Try

object commons {
  def toKebabCase(s: String): String =
    s.replaceAll("([A-Z])", "-$1").toLowerCase.stripPrefix("-")

  implicit def covariantAnyFunctor[F[_]](implicit
      ev: MonadError[F, ConstraintError]
  ): Covariant[F] =
    new Covariant[F] {
      def map[A, B](fa: F[A])(f: A => B): F[B] = Functor[F].map(fa)(f)
    }

  def parseJson[A](schema: Schema[A]): String => Either[String, A] = {
    val decoder = smithy4s.json.Json.payloadCodecs.decoders.fromSchema(schema)
    s =>
      decoder
        .decode(Blob(s))
        .leftMap(pe => pe.toString)
  }

  implicit val docArgument: Argument[Document] = {
    val parse = parseJson(Schema.document)

    Argument.from("json")(parse(_).toValidatedNel)
  }
  val blobArgument: Argument[Blob] = {
    val decoder = Base64.getDecoder
    Argument.from("base64")(s => Valid(Blob(decoder.decode(s))))
  }
  val localDateArgument: Argument[LocalDate] =
    Argument.from("localDate") { s =>
      LocalDate
        .parse(s)
        .toValidNel(
          s"""Invalid localDate "$s". Expected format: ${DateTimeFormatter.ISO_LOCAL_DATE}"""
        )
    }

  val localTimeArgument: Argument[LocalTime] =
    Argument.from("localTime") { s =>
      LocalTime
        .parse(s)
        .toValidNel(
          s"""Invalid localTime "$s". Expected format: ${DateTimeFormatter.ISO_LOCAL_TIME}"""
        )
    }

  val durationArgument: Argument[Duration] =
    Argument.from("duration") { s =>
      Try(s.toLong).toOption match {
        case Some(seconds) => Valid(Duration(seconds, "seconds"))
        case None =>
          Try(Duration(s)).toOption.toValidNel(s"""Invalid duration "$s".""")
      }
    }

  val offsetDateTimeArgument: Argument[OffsetDateTime] =
    Argument.from("offsetDateTime") { s =>
      OffsetDateTime
        .parse(s)
        .toValidNel(
          s"""Invalid localTime "$s". Expected format: ${DateTimeFormatter.ISO_OFFSET_DATE_TIME}"""
        )
    }
}

final case class RefinementFailed(message: String)
    extends Exception("Refinement failed: " + message)
