package smithy4s.internals

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
  final case class ParameterSegment(
      val value: String,
      terminationChar: Option[Char]
  ) extends PatternSegment(value) {
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
                Some(PatternSegment.ParameterSegment("", None)),
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
                Some(PatternSegment.ParameterSegment("", None)),
                segmentsSoFar :+ s
              )
            else
              loop(remainingStr.tail, Some(s.append(nextChar)), segmentsSoFar)
          case Some(p: PatternSegment.ParameterSegment) =>
            if (nextChar == '}')
              loop(
                remainingStr.tail,
                None,
                segmentsSoFar :+ p.copy(terminationChar =
                  remainingStr.tail.headOption
                )
              )
            else
              loop(remainingStr.tail, Some(p.append(nextChar)), segmentsSoFar)
        }
      case None => segmentsSoFar ++ currentSegment
    }
    loop(str, None, List.empty)
  }

}
