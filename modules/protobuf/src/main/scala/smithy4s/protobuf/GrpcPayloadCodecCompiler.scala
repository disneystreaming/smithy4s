package smithy4s.protobuf

import smithy4s.schema.CachedSchemaCompiler
import smithy4s.codecs.PayloadDecoder
import smithy4s.codecs.PayloadEncoder

trait GrpcPayloadCodecCompiler {
  def decoders: CachedSchemaCompiler[PayloadDecoder]
  def encoders: CachedSchemaCompiler[PayloadEncoder]
}
