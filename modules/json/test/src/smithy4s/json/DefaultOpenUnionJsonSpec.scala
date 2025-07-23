package smithy4s.json

import smithy4s.Blob
import smithy4s.codecs.PayloadError
import smithy4s.example.Foo
import smithy4s.example.SampleOpenUnion
import smithy4s.expect
import smithy4s.Schema

class DefaultOpenUnionJsonSpec extends OpenUnionJsonSpec {

  override def read[A: Schema](blob: Blob): Either[PayloadError, A] =
    Json.read(blob)
  override def write[A: Schema](a: A): Blob =
    Json.writeBlob(a)

  test("open tagged union - fail to decode multiple unknown keys") {
    matches(read[SampleOpenUnion](Blob("""{"foo": "bar", "baz": "qux"}"""))) {
      case Left(ex) =>
        expect(
          ex.getMessage.contains("""Expected no other field after 'foo'""")
        )
    }
  }

  test("tagged union - decoding still fails if invalid tag is present") {
    matches(read[Foo](Blob("""{"foo": "bar"}"""))) { case Left(ex) =>
      expect(
        ex.getMessage.contains("""illegal value of discriminator field "foo"""")
      )
    }
  }

}
