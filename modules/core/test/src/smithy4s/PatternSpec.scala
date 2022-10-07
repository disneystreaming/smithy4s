package smithy4s
import smithy4s.Document
import smithy4s.example._

class PatternSpec extends munit.FunSuite {

  test("Unicode in regex is escaped correctly and can be decoded") {
    val s = "😎"

    val result = Document.Decoder
      .fromSchema(UnicodeRegexString.schema)
      .decode(Document.fromString(s))

    assertEquals(result, Right(UnicodeRegexString(s)))
  }

}
