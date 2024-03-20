package smithy4s.protobuf

import com.google.protobuf.CodedOutputStream
import com.google.protobuf.CodedInputStream
import smithy4s._
import smithy4s.schema.CachedSchemaCompiler
import smithy4s.schema.Schema
import smithy4s.protobuf.internals.TaggedCodec
import smithy4s.protobuf.internals.TaggedCodec._
import alloy.proto.ProtoIndex
import java.io.InputStream
import java.io.OutputStream

// scalafmt: {maxColumn = 120}
trait ProtobufCodec[A] {
  def writeBlob(a: A): Blob
  final def readBlob(blob: Blob): Either[ProtobufReadError, A] = try {
    Right(unsafeReadBlob(blob))
  } catch {
    case e: Throwable => Left(ProtobufReadError(e))
  }
  def unsafeReadBlob(blob: Blob): A
  def unsafeReadInputStream(inputStream: InputStream, closeAfter: Boolean): A
  def writeToOutputStream(a: A, outputStream: OutputStream, bufferSize: Int): Unit
}

object ProtobufCodec extends CachedSchemaCompiler.DerivingImpl[ProtobufCodec] {

  def apply[A](implicit ev: ProtobufCodec[A]): ev.type = ev

  type Aux[A] = TaggedCodec[A]

  def fromSchema[A](schema: Schema[A], cache: Cache): ProtobufCodec[A] = {
    val taggedCodec =
      new smithy4s.protobuf.internals.TaggedCodecSchemaVisitor(cache)(
        schema.addHints(ProtoIndex(1))
      )
    val messageCodec = taggedCodec match {
      case m: MessageCodec[A] => m
      case r: RecursiveCodec[A] => {
        r.suspended.value match {
          case m: MessageCodec[A] => m
          case other              => other.wrap
        }
      }
      case other => other.wrap
    }
    new ProtobufCodec[A] {
      def writeBlob(a: A): Blob = {
        val node = messageCodec.prepareWriteNoTag(a)
        val arr = new Array[Byte](node.serialisedSize)
        val os = CodedOutputStream.newInstance(arr)
        node.write(os)
        os.checkNoSpaceLeft()
        Blob(arr)
      }

      def writeToOutputStream(a: A, outputStream: OutputStream, bufferSize: Int): Unit = {
        val node = messageCodec.prepareWriteNoTag(a)
        val os = CodedOutputStream.newInstance(outputStream, bufferSize)
        node.write(os)
      }

      def unsafeReadBlob(blob: Blob): A = {
        val node = messageCodec.prepareReadNoTag()
        val is = CodecInputStreamPlatform.blobToCodecInputStream(blob)
        node.readOne(is)
        node.complete()
      }

      def unsafeReadInputStream(inputStream: InputStream, closeAfter: Boolean): A = {
        val node = messageCodec.prepareReadNoTag()
        try {
          val is = CodedInputStream.newInstance(inputStream)
          node.readOne(is)
          node.complete()
        } finally {
          if (closeAfter) {
            inputStream.close()
          }
        }
      }
    }
  }

}
