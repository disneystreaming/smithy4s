package smithy4s.protobuf

import smithy4s.Blob
import com.google.protobuf.CodedInputStream

private[protobuf] object CodecInputStreamPlatform {

  private[protobuf] def blobToCodecInputStream(blob: Blob): CodedInputStream =
    blob match {
      case asb: Blob.ArraySliceBlob =>
        CodedInputStream.newInstance(asb.arr, asb.offset, asb.length)
      case bbb: Blob.ByteBufferBlob =>
        CodedInputStream.newInstance(bbb.toArray)
      case qb: Blob.QueueBlob =>
        CodedInputStream.newInstance(qb.toArray)
    }

}
