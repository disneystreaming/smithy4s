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

package smithy4s.internals

import alloy.StructurePattern
import smithy4s._
import smithy4s.http.internals.PathEncode

import scala.util.control.NoStackTrace

private[internals] final case class StructurePatternError(message: String)
    extends RuntimeException(message)
    with NoStackTrace

object StructurePatternRefinementProvider {
  implicit def provider[A](implicit
      sch: Schema[A]
  ): RefinementProvider[StructurePattern, String, A] =
    Refinement.drivenBy[StructurePattern].contextual { c =>
      val de = decode[A](c)
      val en = encode[A](c)
      Surjection[String, A](
        de(_),
        en(_)
      )
    }

  private def decode[A](c: StructurePattern)(implicit
      sch: Schema[A]
  ): String => Either[String, A] = {
    val segments = PatternSegment.segmentsFromString(c.pattern)
    val decoder = new SchemaVisitorPatternDecoder(segments)(sch).getOrElse(
      PatternDecode.raw[A](_ =>
        throw StructurePatternError(
          s"Unable to create decoder for ${sch.shapeId}"
        )
      )
    )
    (input: String) => Right(decoder.decode(input))
  }

  private def encode[A](c: StructurePattern)(implicit
      sch: Schema[A]
  ): A => String = {
    val segments = PatternSegment.segmentsFromString(c.pattern)

    val encoder =
      new SchemaVisitorPatternEncoder(segments)(sch)
        .getOrElse(
          PathEncode.raw[A](_ =>
            throw StructurePatternError(
              s"Unable to create encoder for ${sch.shapeId}"
            )
          )
        )
    (input: A) => {
      val result = encoder.encode(input)
      result.mkString
    }
  }
}
