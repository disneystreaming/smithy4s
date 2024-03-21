package smithy4s.protobuf

import smithy4s.schema._

object Protobuf {

  def codecs: CachedSchemaCompiler[ProtobufCodec] = ProtobufCodec

}
