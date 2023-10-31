package smithy4s.http

import munit._
import smithy4s.schema.Schema
import smithy.api
// import smithy4s.schema.Field

final class MetadataDecoderSpec extends FunSuite {
  test("Optional header") {
    case class Foo(deviceType: Option[String])
    val schema =
      Schema
        .struct(
          Schema.string
            .optional[Foo]("deviceType", _.deviceType)
            .addHints(api.HttpHeader("x-device-type"))
            .addHints(api.Input())
        )(Foo(_))
        .addHints(smithy4s.internals.InputOutput.Input.widen)

    val decoder = Metadata.Decoder.fromSchema(schema)
    val result = decoder.decode(Metadata())

    assertEquals(result, Right(Foo(None)))
  }

  test("Optional bijection header") {
    case class Foo(name: Option[String])
    val schema: Schema[Foo] = {
      val field = Schema.string.option
        .biject[Option[String]](identity[Option[String]](_))(identity(_))
        .required[Foo]("name", _.name)
        .addHints(smithy.api.HttpHeader("X-Name"))
      Schema
        .struct(field)(Foo(_))
        .addHints(smithy4s.internals.InputOutput.Input.widen)
    }

    val decoder = Metadata.Decoder.fromSchema(schema)
    val result = decoder.decode(Metadata())

    assertEquals(result, Right(Foo(None)))
  }
}
