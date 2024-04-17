/*
 *  Copyright 2021-2024 Disney Streaming
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

import com.google.protobuf.CodedInputStream
import smithy4s.Blob

private[protobuf] object CodecInputStreamPlatform {

  private[protobuf] def blobToCodecInputStream(blob: Blob): CodedInputStream =
    blob match {
      case asb: Blob.ArraySliceBlob =>
        CodedInputStream.newInstance(asb.arr, asb.offset, asb.length)
      case bbb: Blob.ByteBufferBlob =>
        CodedInputStream.newInstance(bbb.asByteBufferUnsafe)
      case qb: Blob.QueueBlob =>
        import scala.jdk.CollectionConverters._
        CodedInputStream.newInstance {
          qb.blobs.view.map(_.asByteBufferUnsafe).asJava
        }
    }

}
