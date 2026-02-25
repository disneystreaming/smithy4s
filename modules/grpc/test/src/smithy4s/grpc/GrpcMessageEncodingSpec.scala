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

import weaver.SimpleIOSuite

object GrpcMessageEncodingSpec extends SimpleIOSuite {

  pureTest("percent-encode grpc-message") {
    val input = "hello world"
    val encoded = GrpcMessageEncoding.encode(input)
    val decoded = GrpcMessageEncoding.decode(encoded)
    expect.eql(encoded, "hello world") &&
    expect.same(decoded, Right(input))
  }

  pureTest("invalid percent-encoding fails") {
    val decoded = GrpcMessageEncoding.decode("bad%2G")
    expect.same(
      decoded,
      Left(GrpcMessageEncoding.DecodeError.InvalidPercentEncoding("bad%2G", 3))
    )
  }

  pureTest("roundtrips empty grpc-message") {
    val input = ""
    val encoded = GrpcMessageEncoding.encode(input)
    val decoded = GrpcMessageEncoding.decode(encoded)
    expect.eql(encoded, "") && expect.same(decoded, Right(input))
  }

  pureTest("encode leaves grpc-allowed bytes unencoded") {
    val input = "hello world! a&b=c x(y)z[]{}\""
    val encoded = GrpcMessageEncoding.encode(input)
    val decoded = GrpcMessageEncoding.decode(encoded)
    expect.eql(encoded, input) && expect.same(decoded, Right(input))
  }

  pureTest("encode percent-encodes %") {
    val input = "100% legit"
    val encoded = GrpcMessageEncoding.encode(input)
    val decoded = GrpcMessageEncoding.decode(encoded)
    expect.eql(encoded, "100%25 legit") && expect.same(decoded, Right(input))
  }

  pureTest("percent-encodes unicode characters") {
    val input = "snowman ☃"
    val encoded = GrpcMessageEncoding.encode(input)
    val decoded = GrpcMessageEncoding.decode(encoded)
    expect(decoded == Right(input))
  }
}
