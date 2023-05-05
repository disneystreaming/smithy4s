package smithy4s.internals

import alloy.StructurePattern
import smithy4s._
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
      throw StructurePatternError("Unable to create decoder")
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
          throw StructurePatternError("Unable to create encoder")
        )
    (input: A) => {
      val result = encoder.encode(input)
      result.mkString
    }
  }
}
