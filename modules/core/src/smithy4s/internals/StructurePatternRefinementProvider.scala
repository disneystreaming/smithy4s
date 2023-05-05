package smithy4s.internals

import alloy.StructurePattern
import smithy4s._
import scala.util.control.NoStackTrace
import smithy4s.http.internals.PathEncode

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
