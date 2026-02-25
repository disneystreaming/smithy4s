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

object GrpcTimeoutSpec extends SimpleIOSuite {

  pureTest("parse grpc-timeout units") {
    expect.same(GrpcTimeout.parse("1n"), Right(GrpcTimeout(1, GrpcTimeout.Nanoseconds))) &&
    expect.same(GrpcTimeout.parse("2u"), Right(GrpcTimeout(2, GrpcTimeout.Microseconds))) &&
    expect.same(GrpcTimeout.parse("3m"), Right(GrpcTimeout(3, GrpcTimeout.Milliseconds))) &&
    expect.same(GrpcTimeout.parse("4S"), Right(GrpcTimeout(4, GrpcTimeout.Seconds))) &&
    expect.same(GrpcTimeout.parse("5M"), Right(GrpcTimeout(5, GrpcTimeout.Minutes))) &&
    expect.same(GrpcTimeout.parse("6H"), Right(GrpcTimeout(6, GrpcTimeout.Hours)))
  }

  pureTest("parse invalid grpc-timeout formats") {
    expect(GrpcTimeout.parse("").isLeft) &&
    expect(GrpcTimeout.parse("x").isLeft) &&
    expect(GrpcTimeout.parse("10").isLeft) &&
    expect(GrpcTimeout.parse("10X").isLeft) &&
    expect(GrpcTimeout.parse("0S").isLeft) &&
    expect(GrpcTimeout.parse("000S").isLeft) &&
    expect(GrpcTimeout.parse("123456789S").isLeft) &&
    expect(GrpcTimeout.parse("12345678S").isRight)
  }

  pureTest("grpc-timeout roundtrip header") {
    val timeout = GrpcTimeout(42, GrpcTimeout.Milliseconds)
    expect.same(GrpcTimeout.parse(timeout.toHeader), Right(timeout))
  }
}
