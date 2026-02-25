package smithy4s.grpc

import smithy4s.Blob
import smithy4s.grpc.internals.GrpcMetadataCodec
import weaver.SimpleIOSuite

import java.util.Base64

object GrpcMetadataCodecSpec extends SimpleIOSuite {

  pureTest("decodeBinaryHeaderValue accepts padded base64") {
    val blob = Blob(Array[Byte](1, 2, 3, 4))
    val padded = Base64.getEncoder.encodeToString(blob.toArray)
    expect.same(GrpcMetadataCodec.decodeBinaryHeaderValue(padded), Right(Vector(blob)))
  }

  pureTest("decodeBinaryHeaderValue accepts unpadded base64") {
    val blob = Blob(Array[Byte](5, 6, 7, 8))
    val unpadded = Base64.getEncoder.withoutPadding().encodeToString(blob.toArray)
    expect.same(GrpcMetadataCodec.decodeBinaryHeaderValue(unpadded), Right(Vector(blob)))
  }

  pureTest("decodeBinaryHeaderValue splits comma-joined values, keeping empties") {
    val first = Blob(Array[Byte](1, 2, 3))
    val second = Blob(Array[Byte](4, 5))
    val firstEncoded = Base64.getEncoder.withoutPadding().encodeToString(first.toArray)
    val secondEncoded = Base64.getEncoder.encodeToString(second.toArray)
    val headerValue = s"$firstEncoded,, $secondEncoded"
    expect.same(
      GrpcMetadataCodec.decodeBinaryHeaderValue(headerValue),
      Right(Vector(first, Blob.empty, second))
    )
  }

  pureTest("decodeBinaryHeaderValue trims whitespace around tokens") {
    val first = Blob(Array[Byte](9, 10))
    val second = Blob(Array[Byte](11, 12, 13))
    val firstEncoded = Base64.getEncoder.withoutPadding().encodeToString(first.toArray)
    val secondEncoded = Base64.getEncoder.encodeToString(second.toArray)
    val headerValue = s"  $firstEncoded ,  $secondEncoded  "
    expect.same(
      GrpcMetadataCodec.decodeBinaryHeaderValue(headerValue),
      Right(Vector(first, second))
    )
  }
}
