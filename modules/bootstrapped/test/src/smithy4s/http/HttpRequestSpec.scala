package smithy4s.http

import munit._
import smithy4s.schema.OperationSchema
import smithy4s.schema.Schema
import smithy4s._
import smithy.api

final class HttpRequestSpec extends FunSuite {

  test("host prefix") {
    case class Foo(foo: String)
    val schema =
      Schema.struct(Schema.string.required[Foo]("foo", _.foo))(Foo(_))
    val endpointHint =
      api.Endpoint(hostPrefix = api.NonEmptyString("test.{foo}-other."))
    val opSchema = OperationSchema(
      ShapeId("test", "Test"),
      Hints(endpointHint),
      schema,
      None,
      Schema.unit,
      None,
      None
    )

    val writer = HttpRequest.Writer.hostPrefix[String, Foo](opSchema)

    val uri = HttpUri(
      HttpUriScheme.Https,
      "example.com",
      None,
      Seq.empty,
      Map.empty,
      None
    )
    val request = HttpRequest(HttpMethod.GET, uri, Map.empty, "")
    val resultUri = writer.write(request, Foo("hello")).uri
    assertEquals(resultUri, uri.copy(host = "test.hello-other.example.com"))
  }

}
