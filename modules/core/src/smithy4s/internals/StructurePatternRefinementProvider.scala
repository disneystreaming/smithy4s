package smithy4s.internals

import alloy.StructurePattern
import smithy4s._
import annotation.tailrec

sealed abstract class PatternSegment(value: String)
    extends Product
    with Serializable {
  def append(char: Char): PatternSegment
}
object PatternSegment {
  final case class StaticSegment(val value: String)
      extends PatternSegment(value) {
    override def append(char: Char): PatternSegment =
      this.copy(value = this.value + char)
  }
  final case class ParameterSegment(val value: String)
      extends PatternSegment(value) {
    override def append(char: Char): PatternSegment =
      this.copy(value = this.value + char)
  }

  def segmentsFromString(str: String): List[PatternSegment] = {
    @tailrec
    def loop(
        remainingStr: String,
        currentSegment: Option[PatternSegment],
        segmentsSoFar: List[PatternSegment]
    ): List[PatternSegment] = remainingStr.headOption match {
      case Some(nextChar) =>
        currentSegment match {
          case None =>
            if (nextChar == '{')
              loop(
                remainingStr.tail,
                Some(PatternSegment.ParameterSegment("")),
                segmentsSoFar
              )
            else
              loop(
                remainingStr.tail,
                Some(PatternSegment.StaticSegment(nextChar.toString)),
                segmentsSoFar
              )
          case Some(s: PatternSegment.StaticSegment) =>
            if (nextChar == '{')
              loop(
                remainingStr.tail,
                Some(PatternSegment.ParameterSegment("")),
                segmentsSoFar :+ s
              )
            else
              loop(remainingStr.tail, Some(s.append(nextChar)), segmentsSoFar)
          case Some(p: PatternSegment.ParameterSegment) =>
            if (nextChar == '}')
              loop(remainingStr.tail, None, segmentsSoFar :+ p)
            else
              loop(remainingStr.tail, Some(p.append(nextChar)), segmentsSoFar)
        }
      case None => segmentsSoFar ++ currentSegment
    }
    loop(str, None, List.empty)
  }

}

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

  private def decode[A](c: StructurePattern): String => Either[String, A] =
    _ => Left("")

  private def encode[A](c: StructurePattern)(implicit
      sch: Schema[A]
  ): A => String = {
    val segments = PatternSegment.segmentsFromString(c.pattern)

    val encoder =
      new SchemaVisitorPatternEncoder(segments)(sch)
        .getOrElse(
          throw new Exception("Unable to encode") // TODO: Better error here
        )
    input: A => {
      val result = encoder.encode(input)
      result.mkString
    }
  }
}
