package smithy4s
package scalacheck

import org.scalacheck.Gen

private[scalacheck] object Smithy4sGen {

  private val year = Gen.chooseNum(1900, 2100)
  private val month = Gen.chooseNum(1, 12)
  private val hour = Gen.chooseNum(0, 23)
  // private val nanos = Gen.chooseNum(0, math.pow(10, 8).toInt)
  val minute, second = Gen.chooseNum(0, 59)

  private def isLeap(year: Int) =
    (year % 4 == 0) && (year % 100 != 0) || (year % 400 == 0)

  private def day(year: Int, month: Int) = month match {
    case 2 if isLeap(year) => Gen.chooseNum(1, 29)
    case 2                 => Gen.chooseNum(1, 28)
    case 4 | 6 | 9 | 11    => Gen.chooseNum(1, 30)
    case _                 => Gen.chooseNum(1, 31)
  }

  val genTimestamp: Gen[Timestamp] = for {
    YYYY <- year
    MM <- month
    DD <- day(YYYY, MM)
    hh <- hour
    mm <- minute
    ss <- second
  } yield Timestamp(YYYY, MM, DD, hh, mm, ss, 0)

  def genDocument(maxDepth: Int): Gen[Document] = if (maxDepth <= 0) {
    Gen.oneOf(
      Gen.oneOf(Document.DBoolean(true), Document.DBoolean(false)),
      Gen
        .chooseNum(Double.MinValue, Double.MaxValue)
        .map(BigDecimal(_))
        .map(Document.DNumber(_)),
      Gen.const(Document.DNull),
      Gen.alphaStr.map(Document.fromString)
    )
  } else {
    val nextDepth = Gen.chooseNum(0, maxDepth - 1)
    val nextLayer = nextDepth.flatMap(genDocument)
    val genArray =
      Gen.listOf(nextLayer).map(_.toIndexedSeq).map(Document.DArray(_))
    val genObj =
      Gen.mapOf(Gen.zip(Gen.identifier, nextLayer)).map(Document.DObject(_))
    Gen.oneOf(genArray, genObj)
  }

}
