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

package smithy4s.protobuf

import smithy4s.ShapeId
import weaver.SimpleIOSuite

object ProtobufReadErrorSpec extends SimpleIOSuite {
  private val codec =
    ProtobufCodec.fromSchema(ProtobufReadError.schema, ProtobufCodec.createCache())

  pureTest("ProtobufReadError schema roundtrips MissingRequiredField") {
    val error = ProtobufReadError.MissingRequiredField(
      ShapeId("example", "Input"),
      "field",
      1
    )
    val decoded = codec.readBlob(codec.writeBlob(error))
    expect.same(decoded, Right(error))
  }

  pureTest("ProtobufReadError schema roundtrips ViolatedConstraint") {
    val error = ProtobufReadError.ViolatedConstraint(
      smithy.api.Length(min = Some(1), max = None),
      "too short"
    )
    val decoded = codec.readBlob(codec.writeBlob(error))
    expect.same(decoded, Right(error))
  }

  pureTest("ProtobufReadError schema roundtrips Other") {
    val error = ProtobufReadError.Other(new RuntimeException("boom"))
    val decoded = codec.readBlob(codec.writeBlob(error))

    expect.same(decoded.map(_.getMessage), Right(error.getMessage))
  }
}
