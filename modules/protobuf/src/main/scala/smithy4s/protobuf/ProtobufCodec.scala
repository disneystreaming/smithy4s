/*
 *  Copyright 2021-2025 Disney Streaming
 *
 *  Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     https://disneystreaming.github.io/TOST-1.0.txt
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package smithy4s.protobuf

import alloy.proto.ProtoIndex
import com.google.protobuf.CodedInputStream
import com.google.protobuf.CodedOutputStream
import smithy4s._
import smithy4s.protobuf.internals.TaggedCodec
import smithy4s.protobuf.internals.TaggedCodec._
import smithy4s.schema.CachedSchemaCompiler
import smithy4s.schema.Schema

import java.io.InputStream
import java.io.OutputStream

// scalafmt: {maxColumn = 120}
trait ProtobufCodec[A] {
  def writeBlob(a: A): Blob
  final def readBlob(blob: Blob): Either[ProtobufReadError, A] = try {
    Right(unsafeReadBlob(blob))
  } catch {
    case e: ProtobufReadError           => Left(e)
    case scala.util.control.NonFatal(e) => Left(ProtobufReadError.Other(e))
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
