package smithy4s.http.json

import munit.FunSuite
import smithy.api.JsonName
import smithy4s.schema.Schema
import smithy4s.HintMask

class JsonCodecApiTests extends FunSuite {

  test(
    "codecs with an empty hint mask should not be affected by format hints"
  ) {
    val schemaWithJsonName = Schema
      .struct[String]
      .apply(
        Schema.string
          .addHints(JsonName("b"))
          .required[String]("a", identity)
      )(identity)

    val capi = codecs(HintMask.empty)

    val codec = capi.compileCodec(schemaWithJsonName)
    val encodedString = new String(capi.writeToArray(codec, "test"))

    assertEquals(encodedString, """{"a":"test"}""")
  }

  test(
    "struct codec with a required field should return a Left when the field is missing"
  ) {
    val schemaWithRequiredField =
      Schema
        .struct[String]
        .apply(
          Schema.string
            .required[String]("a", identity)
        )(identity)

    val capi = codecs(HintMask.empty)

    val codec = capi.compileCodec(schemaWithRequiredField)

    val decoded = capi.decodeFromByteArray(codec, """{}""".getBytes())

    assert(decoded.isLeft)
  }
}
