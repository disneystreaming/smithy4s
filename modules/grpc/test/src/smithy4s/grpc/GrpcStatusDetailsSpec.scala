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
import smithy4s.protobuf.ProtobufCodec
import weaver.SimpleIOSuite

object GrpcStatusDetailsSpec extends SimpleIOSuite {

  private val errorPayloadCodec =
    ProtobufCodec.fromSchema(ErrorPayload.schema, ProtobufCodec.createCache())

  pureTest("encode and decode ErrorPayload") {
    val status = ErrorPayload(
      code = Some(3),
      message = Some("invalid"),
      details = Some(
        List(
          StatusDetails(
            typeUrl = "example.Type",
            value = Blob(Array[Byte](1, 2, 3))
          )
        )
      )
    )
    val encoded = errorPayloadCodec.writeBlob(status)
    val decoded = errorPayloadCodec.readBlob(encoded)
    expect.same(decoded, Right(status))
  }

  pureTest("encode and decode ErrorPayload with empty details") {
    val status = ErrorPayload(
      code = Some(0),
      message = None,
      details = Some(Nil)
    )
    val encoded = errorPayloadCodec.writeBlob(status)
    val decoded = errorPayloadCodec.readBlob(encoded)
    val details = decoded.map(_.details.getOrElse(Nil))
    expect.same(details, Right(Nil))
  }

  pureTest("encode and decode ErrorPayload without details") {
    val status = ErrorPayload(
      code = None,
      message = Some("ok"),
      details = None
    )
    val encoded = errorPayloadCodec.writeBlob(status)
    val decoded = errorPayloadCodec.readBlob(encoded)
    expect.same(decoded, Right(status))
  }

  pureTest("encode and decode ErrorPayload with multiple details") {
    val status = ErrorPayload(
      code = Some(7),
      message = Some("denied"),
      details = Some(
        List(
          StatusDetails("example.One", Blob(Array[Byte](1))),
          StatusDetails("example.Two", Blob(Array[Byte](2, 3)))
        )
      )
    )
    val encoded = errorPayloadCodec.writeBlob(status)
    val decoded = errorPayloadCodec.readBlob(encoded)
    expect.same(decoded, Right(status))
  }
}
