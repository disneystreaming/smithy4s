package smithy4s.json

import smithy4s.Blob
import smithy4s.codecs.PayloadError
import smithy4s.Document
import smithy4s.Schema

class DefaultOpenUnionJsonSpec extends OpenUnionJsonSpec {

  override def read[A: Schema](blob: Blob): Either[PayloadError, A] =
    Json.read(blob)
  override def write[A: Schema](a: A): Blob =
    Json.writeBlob(a)

}
