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

package smithy4s.grpc.http4s

import cats.effect.IO
import org.http4s.{Header, Headers}
import org.typelevel.ci.CIString
import smithy4s.Blob
import smithy4s.grpc._
import smithy4s.grpc.http4s.internals.GrpcHttp4sMetadata
import smithy4s.grpc.internals.GrpcConstants
import smithy4s.interopcats._
import weaver.SimpleIOSuite

import java.util.Base64

object GrpcHttp4sMetadataSpec extends SimpleIOSuite {

  test("binary metadata round-trips") {
    val original = GrpcMetadata.empty.addBinary("test-bin", Blob(Array[Byte](1, 2, 3)))
    val headers  = GrpcHttp4sMetadata.toHeaders(original)
    GrpcHttp4sMetadata.fromApplicationHeaders[IO](headers).map { roundTripped =>
      expect.same(roundTripped, original)
    }
  }

  test("binary metadata accepts padded base64") {
    val blob    = Blob(Array[Byte](1, 2, 3, 4))
    val padded  = Base64.getEncoder.encodeToString(blob.toArray)
    val headers = Headers(Header.Raw(CIString("test-bin"), padded))
    GrpcHttp4sMetadata.fromApplicationHeaders[IO](headers).map { metadata =>
      expect.same(metadata.getBinary("test-bin"), Vector(blob))
    }
  }

  test("binary metadata accepts unpadded base64") {
    val blob     = Blob(Array[Byte](1, 2, 3, 4))
    val unpadded = Base64.getEncoder.withoutPadding().encodeToString(blob.toArray)
    val headers  = Headers(Header.Raw(CIString("test-bin"), unpadded))
    GrpcHttp4sMetadata.fromApplicationHeaders[IO](headers).map { metadata =>
      expect.same(metadata.getBinary("test-bin"), Vector(blob))
    }
  }

  test("binary metadata splits comma-joined values keeping empties") {
    val first   = Blob(Array[Byte](1, 2, 3))
    val second  = Blob(Array[Byte](4, 5))
    val enc1    = Base64.getEncoder.withoutPadding().encodeToString(first.toArray)
    val enc2    = Base64.getEncoder.encodeToString(second.toArray)
    val headers = Headers(Header.Raw(CIString("test-bin"), s"$enc1,, $enc2"))
    GrpcHttp4sMetadata.fromApplicationHeaders[IO](headers).map { metadata =>
      expect.same(metadata.getBinary("test-bin"), Vector(first, Blob.empty, second))
    }
  }

  test("fromApplicationHeaders strips protocol headers") {
    val binary        = Blob(Array[Byte](1, 2, 3))
    val binaryEncoded = Base64.getEncoder.withoutPadding().encodeToString(binary.toArray)
    val headers = Headers(
      Header.Raw(CIString("x-foo"), "bar"),
      Header.Raw(CIString("x-bin-bin"), binaryEncoded),
      Header.Raw(CIString(GrpcConstants.grpcStatusHeader), "0"),
      Header.Raw(CIString(GrpcConstants.grpcMessageHeader), "ok"),
      Header.Raw(CIString(GrpcConstants.grpcEncodingHeader), "gzip"),
      Header.Raw(CIString(GrpcConstants.grpcAcceptEncodingHeader), "gzip"),
      Header.Raw(CIString(GrpcConstants.grpcTimeoutHeader), "1S"),
      Header.Raw(CIString(GrpcConstants.grpcStatusDetailsHeader), "AAAA"),
      Header.Raw(CIString("content-type"), "application/grpc+proto"),
      Header.Raw(CIString("te"), "trailers"),
      Header.Raw(CIString(":status"), "200")
    )
    GrpcHttp4sMetadata.fromApplicationHeaders[IO](headers).map { metadata =>
      expect.same(metadata.getText("x-foo"), Vector("bar")) &&
      expect.same(metadata.getBinary("x-bin-bin"), Vector(binary)) &&
      expect(metadata.getText(GrpcConstants.grpcStatusHeader).isEmpty) &&
      expect(metadata.getText(GrpcConstants.grpcMessageHeader).isEmpty) &&
      expect(metadata.getText(GrpcConstants.grpcEncodingHeader).isEmpty) &&
      expect(metadata.getText(GrpcConstants.grpcAcceptEncodingHeader).isEmpty) &&
      expect(metadata.getText(GrpcConstants.grpcTimeoutHeader).isEmpty) &&
      expect(metadata.getBinary(GrpcConstants.grpcStatusDetailsHeader).isEmpty) &&
      expect(metadata.getText("content-type").isEmpty) &&
      expect(metadata.getText("te").isEmpty) &&
      expect(metadata.getText(":status").isEmpty)
    }
  }

  test("fromTrailers preserves grpc fields and drops pseudo-headers") {
    val details        = Blob(Array[Byte](4, 5, 6))
    val detailsEncoded = Base64.getEncoder.withoutPadding().encodeToString(details.toArray)
    val headers = Headers(
      Header.Raw(CIString(GrpcConstants.grpcStatusHeader), "7"),
      Header.Raw(CIString(GrpcConstants.grpcMessageHeader), "denied"),
      Header.Raw(CIString(GrpcConstants.grpcStatusDetailsHeader), detailsEncoded),
      Header.Raw(CIString("x-trailer"), "ok"),
      Header.Raw(CIString(":status"), "200")
    )
    GrpcHttp4sMetadata.fromTrailers[IO](headers).map { metadata =>
      expect.same(metadata.getText(GrpcConstants.grpcStatusHeader), Vector("7")) &&
      expect.same(metadata.getText(GrpcConstants.grpcMessageHeader), Vector("denied")) &&
      expect.same(metadata.getBinary(GrpcConstants.grpcStatusDetailsHeader), Vector(details)) &&
      expect.same(metadata.getText("x-trailer"), Vector("ok")) &&
      expect(metadata.getText(":status").isEmpty)
    }
  }
}
