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

import cats.effect.IO
import smithy4s.Blob
import smithy4s.interopcats._
import weaver.SimpleIOSuite

object GrpcFrameSpec extends SimpleIOSuite {

  test("encode and decode a single frame") {
    val frame = GrpcFrame(compressed = false, Blob(Array[Byte](1, 2, 3)))
    val bytes = GrpcFrame.encode(frame)
    GrpcFrame.decodeUnary[IO](Blob(bytes), 1024).map { decoded =>
      expect.eql(bytes.take(5).toVector, Vector[Byte](0, 0, 0, 0, 3)) &&
      expect.eql(decoded.compressed, false) &&
      expect(decoded.message.sameBytesAs(frame.message))
    }
  }

  test("invalid compression flag fails decoding") {
    val bytes = Vector[Byte](2, 0, 0, 0, 0)
    GrpcFrame.decodeUnary[IO](Blob(bytes.toArray), 1024).attempt.map { result =>
      expect.same(result, Left(GrpcFailure.InvalidFrame(GrpcFrame.Error.InvalidCompressionFlag)))
    }
  }

  test("decodeUnary succeeds at max size") {
    val max = 3
    val frame = GrpcFrame(compressed = false, Blob(Array[Byte](1, 2, 3)))
    val bytes = GrpcFrame.encode(frame)
    GrpcFrame.decodeUnary[IO](Blob(bytes), max).map { decoded =>
      expect.same(decoded, frame)
    }
  }

  test("decodeUnary fails over max size") {
    val max = 3
    val frame = GrpcFrame(compressed = false, Blob(Array[Byte](1, 2, 3, 4)))
    val bytes = GrpcFrame.encode(frame)
    GrpcFrame.decodeUnary[IO](Blob(bytes), max).attempt.map { result =>
      expect.same(result, Left(GrpcFailure.InvalidFrame(GrpcFrame.Error.MessageTooLarge)))
    }
  }

  test("decodeUnary fails on empty input") {
    val decoded = GrpcFrame.decodeUnary[IO](Blob.empty, 16)
    decoded.attempt.map { result =>
      expect.same(result, Left(GrpcFailure.InvalidFrame(GrpcFrame.Error.MissingFrame)))
    }
  }

  test("decodeUnary fails on multiple frames") {
    val frame1 = GrpcFrame(compressed = false, Blob(Array[Byte](1)))
    val frame2 = GrpcFrame(compressed = false, Blob(Array[Byte](2)))
    val bytes1 = GrpcFrame.encode(frame1)
    val bytes2 = GrpcFrame.encode(frame2)
    GrpcFrame.decodeUnary[IO](Blob(bytes1 ++ bytes2), 16).attempt.map { result =>
      expect.same(result, Left(GrpcFailure.InvalidFrame(GrpcFrame.Error.MultipleFrames)))
    }
  }

  test("decodeUnary treats 2GiB length as too large") {
    val header = Array[Byte](0, 0x80.toByte, 0, 0, 0)
    GrpcFrame.decodeUnary[IO](Blob(header), Int.MaxValue).attempt.map { result =>
      expect.same(result, Left(GrpcFailure.InvalidFrame(GrpcFrame.Error.MessageTooLarge)))
    }
  }

  test("decodeUnary treats max uint32 length as too large") {
    val header = Array[Byte](0, 0xff.toByte, 0xff.toByte, 0xff.toByte, 0xff.toByte)
    GrpcFrame.decodeUnary[IO](Blob(header), Int.MaxValue).attempt.map { result =>
      expect.same(result, Left(GrpcFailure.InvalidFrame(GrpcFrame.Error.MessageTooLarge)))
    }
  }

  test("decodeUnary still reports truncated message for large valid lengths") {
    val header = Array[Byte](0, 0x7f.toByte, 0xff.toByte, 0xff.toByte, 0xff.toByte)
    GrpcFrame.decodeUnary[IO](Blob(header), Int.MaxValue).attempt.map { result =>
      expect.same(result, Left(GrpcFailure.InvalidFrame(GrpcFrame.Error.TruncatedMessage)))
    }
  }

  test("decodeSingle fails on truncated header") {
    val decoded = GrpcFrame.decodeUnary[IO](Blob(Array[Byte](0, 0, 0)), 16)
    decoded.attempt.map { result =>
      expect.same(result, Left(GrpcFailure.InvalidFrame(GrpcFrame.Error.TruncatedHeader)))
    }
  }

  test("decodeSingle fails on truncated message") {
    val decoded = GrpcFrame.decodeUnary[IO](Blob(Array[Byte](0, 0, 0, 0, 2, 1)), 16)
    decoded.attempt.map { result =>
      expect.same(result, Left(GrpcFailure.InvalidFrame(GrpcFrame.Error.TruncatedMessage)))
    }
  }
}
