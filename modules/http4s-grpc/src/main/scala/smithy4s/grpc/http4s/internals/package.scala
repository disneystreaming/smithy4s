package smithy4s.grpc.http4s

import smithy4s.kinds.PolyFunction
import smithy4s.codecs.BlobDecoder
import smithy4s.codecs.BlobEncoder
import smithy4s.Blob
import java.nio.ByteBuffer

package object internals {

  private val PAYLOAD_PREFIX_SIZE = 5

  private[internals] def lengthPrefixDecoder = new PolyFunction[BlobDecoder, BlobDecoder] {
    override def apply[A0](fa: BlobDecoder[A0]): BlobDecoder[A0] = {
      fa.compose{in =>
        val newBufferSize = in.size - PAYLOAD_PREFIX_SIZE
        val bb = ByteBuffer.allocate(newBufferSize)
        in.copyToBuffer(bb, 5, newBufferSize)
        bb.rewind()
        Blob(bb)
      }
    }
  }

  private[internals] def lengthPrefixEncoder = new PolyFunction[BlobEncoder, BlobEncoder] {
      override def apply[A0](fa: BlobEncoder[A0]): BlobEncoder[A0] = fa.andThen{ payloadBlob =>
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
