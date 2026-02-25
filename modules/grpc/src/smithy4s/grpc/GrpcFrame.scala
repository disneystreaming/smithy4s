/*
 *  Copyright 2021-2026 Disney Streaming
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

package smithy4s.grpc

import smithy4s.Blob
import smithy4s.capability._
import smithy4s.grpc.GrpcFailure.InvalidFrame

final case class GrpcFrame(compressed: Boolean, message: Blob)

object GrpcFrame {
  object Error {
    val MissingFrame = "Missing frame"
    val TruncatedHeader = "Truncated gRPC frame header"
    val TruncatedMessage = "Truncated gRPC frame message"
    val InvalidCompressionFlag = "Invalid gRPC compression flag"
    val MessageTooLarge = "gRPC message exceeds max size"
    val MultipleFrames = "Multiple gRPC frames in unary request"
  }

  def decodeUnary[F[_]](bytes: Blob, maxMessageSize: Int)(implicit F: MonadThrowLike[F]): F[GrpcFrame] = {
    val dataLength = bytes.size.toLong
    if (dataLength == 0) F.raiseError(InvalidFrame(Error.MissingFrame))
    else if (dataLength < 5) F.raiseError(InvalidFrame(Error.TruncatedHeader))
    else {
      val flag = bytes(0)
      val lengthLong = readLength(bytes)
      if (flag != 0 && flag != 1) F.raiseError(InvalidFrame(Error.InvalidCompressionFlag))
      // Check size before casting to Int or slicing to avoid overflow and large allocations.
      else if (lengthLong > maxMessageSize.toLong) F.raiseError(InvalidFrame(Error.MessageTooLarge))
      else if (lengthLong > Int.MaxValue.toLong) F.raiseError(InvalidFrame(Error.MessageTooLarge))
      else {
        // Use Long arithmetic to avoid overflow when lengthLong is near Int.MaxValue.
        val expectedSize = 5L + lengthLong
        if (dataLength < expectedSize) F.raiseError(InvalidFrame(Error.TruncatedMessage))
        else if (dataLength > expectedSize) F.raiseError(InvalidFrame(Error.MultipleFrames))
        else {
          val length = lengthLong.toInt
          val message = Array.ofDim[Byte](length)
          bytes.copyToArray(message, 0, 5, length)
          F.pure(GrpcFrame(flag == 1, Blob(message)))
        }
      }
    }
  }

  def encode(frame: GrpcFrame): Array[Byte] = {
    val messageSize = frame.message.size
    val header = encodeHeader(frame.compressed, messageSize) // Array[Byte] length 5
    val out = new Array[Byte](header.length + messageSize)
    System.arraycopy(header, 0, out, 0, header.length)
    frame.message.copyToArray(out, header.length, 0, messageSize)
    out
  }

  private def encodeHeader(compressed: Boolean, length: Int): Array[Byte] = {
    val header = new Array[Byte](5)
    header(0) = if (compressed) 1.toByte else 0.toByte
    header(1) = ((length >>> 24) & 0xff).toByte
    header(2) = ((length >>> 16) & 0xff).toByte
    header(3) = ((length >>> 8) & 0xff).toByte
    header(4) = (length & 0xff).toByte
    header
  }

  // gRPC encodes the message length as an unsigned 32-bit big-endian integer.
  // We return a Long to avoid signed Int overflow and to compare safely before casting.
  private def readLength(bytes: Blob): Long =
    ((bytes(1) & 0xffL) << 24) |
      ((bytes(2) & 0xffL) << 16) |
      ((bytes(3) & 0xffL) << 8) |
      (bytes(4) & 0xffL)
}
