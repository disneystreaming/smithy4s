package smithy4s
package protobuf
package internals

import smithy4s.codecs.PayloadDecoder
import smithy4s.codecs.PayloadEncoder
import smithy4s.schema.CachedSchemaCompiler
import smithy4s.schema.Schema
import smithy4s.protobuf.ProtobufCodec
import smithy4s.Blob
import java.nio.ByteBuffer
import smithy4s.codecs
import smithy4s.protobuf.ProtobufReadError.Other
import smithy4s.protobuf.ProtobufReadError.MissingRequiredField
import smithy4s.protobuf.ProtobufReadError.ViolatedConstraint

class GrpcPayloadCodecCompilerImpl(protoCodecCompiler: CachedSchemaCompiler[ProtobufCodec]) extends GrpcPayloadCodecCompiler {
  private val PAYLOAD_PREFIX_SIZE = 5
  override def decoders: CachedSchemaCompiler[PayloadDecoder] = new CachedSchemaCompiler[PayloadDecoder] {

    type Cache = protoCodecCompiler.Cache

    override def createCache(): Cache = {
      protoCodecCompiler.createCache()
    }

    override def fromSchema[A](schema: Schema[A]): PayloadDecoder[A] = fromSchema(schema, createCache())

    override def fromSchema[A](schema: Schema[A], cache: Cache): PayloadDecoder[A] = {
      val pCodec = protoCodecCompiler.fromSchema(schema, cache)
      input => {
        Either.cond(input.size > PAYLOAD_PREFIX_SIZE, input, codecs.PayloadError(codecs.PayloadPath.root, "Invalid payload prefix", "Invalid payload prefix"))
          .flatMap{ blob =>
            val messageWithoutPrefix = Blob(blob.asByteBuffer(5, blob.size - 5))
            pCodec.readBlob(messageWithoutPrefix)
                      .left
                      .map{
                        case Other(cause) => codecs.PayloadError(
                          codecs.PayloadPath.root,
                          "Unexpected error",
                          cause.getMessage()
                        )
                        case error:MissingRequiredField =>
                          codecs.PayloadError(
                            codecs.PayloadPath.root,
                            "Missing required field",
                            error.toString()
                          )
                        case error:ViolatedConstraint =>
                          codecs.PayloadError(
                            codecs.PayloadPath.root,
                            "Constraint violation error",
                            error.toString()
                          )
                      }
          }
      }
    }

  }

  override def encoders: CachedSchemaCompiler[PayloadEncoder] = {
    new CachedSchemaCompiler[PayloadEncoder] {

      type Cache = protoCodecCompiler.Cache

      override def createCache(): Cache = protoCodecCompiler.createCache()

      override def fromSchema[A](schema: Schema[A]): PayloadEncoder[A] = fromSchema(schema, createCache())

      override def fromSchema[A](schema: Schema[A], cache: Cache): PayloadEncoder[A] = {
        val pCodec = protoCodecCompiler.fromSchema(schema, cache)
        (value: A) => {         
          val payloadBlob = pCodec.writeBlob(value)

          val compressionFlag: Byte = 0 //if (compressed) 1 else 0
          val messageLength = payloadBlob.size

          // Create 5-byte header + message
          val messageWithGrpcPrefix = 
            ByteBuffer.allocate(PAYLOAD_PREFIX_SIZE + messageLength)
              .put(compressionFlag)           // 1 byte: compression flag
              .putInt(messageLength)          // 4 bytes: message length (big-endian)
              .put(payloadBlob.toArray)       // N bytes: actual protobuf message
              .rewind().asInstanceOf[ByteBuffer]

          Blob(messageWithGrpcPrefix)
        }
      }
    }
  }
}
