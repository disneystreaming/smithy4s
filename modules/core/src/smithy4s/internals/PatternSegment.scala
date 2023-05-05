package smithy4s.internals

import scala.collection.mutable.{ListBuffer, ArrayBuffer}

private[internals] sealed abstract class PatternSegment(value: String)
    extends Product
    with Serializable {
  def append(char: Char): PatternSegment
}

private[internals] object PatternSegment {
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
    val segmentsSoFar = ListBuffer.empty[PatternSegment]
    val currentSegment = ArrayBuffer.empty[Char]
    var isInsideParameterSegment = false
    for ((nextChar, i) <- str.zipWithIndex) {
      if (isInsideParameterSegment) {
        if (nextChar == '}') {
          isInsideParameterSegment = false
          val terminationChar =
            if (i + 1 < str.length) Some(str.charAt(i + 1)) else None
          segmentsSoFar.append(
            PatternSegment.ParameterSegment(
              new String(currentSegment.toArray),
              terminationChar
            )
          )
          currentSegment.clear()

        } else {
          currentSegment.append(nextChar)
        }
      } else {
        if (nextChar == '{') {
          isInsideParameterSegment = true
          if (currentSegment.nonEmpty) {
            segmentsSoFar.append(
              PatternSegment.StaticSegment(new String(currentSegment.toArray))
            )
            currentSegment.clear()
          }
        } else {
          currentSegment.append(nextChar)
        }
      }
    }
    if (!isInsideParameterSegment && currentSegment.nonEmpty) {
      segmentsSoFar.append(
        PatternSegment.StaticSegment(new String(currentSegment.toArray))
      )
    }
    segmentsSoFar.toList
  }

}
