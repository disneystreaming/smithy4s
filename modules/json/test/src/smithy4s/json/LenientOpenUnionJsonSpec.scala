package smithy4s.json

import smithy4s.Blob
import smithy4s.codecs.PayloadError
import smithy4s.Document
import smithy4s.Schema

class LenientOpenUnionJsonSpec extends OpenUnionJsonSpec {

  val payloadCompiler = smithy4s.json.internals.JsonPayloadCodecCompilerImpl.defaultJsonPayloadCodecCompiler.configureJsoniterCodecCompiler(
    _.withLenientTaggedUnionDecoding
  )

  override def read[A: Schema](blob: Blob): Either[PayloadError, A] =
    payloadCompiler.decoders.fromSchema(Schema[A]).decode(blob)
  override def write[A: Schema](a: A): Blob = 
    payloadCompiler.encoders.fromSchema(Schema[A]).encode(a)

}
