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

import munit._
import smithy4s.Blob
import smithy4s.Document
import smithy4s.Timestamp
import smithy4s.example.protobuf
import smithy4s.schema.Schema

import java.util.UUID

// A few tests utilising java code-generated classes that ScalaPB doesn't have a pure scala version of.
class JVMCodecTests() extends FunSuite {

  test("UUID") {
    val uuid1 = UUID.randomUUID()
    val uuid2 = smithy4s.example.protobuf.CompactUUID(UUID.randomUUID())
    val uuids = protobuf.UUIDWrapper(
      Some(uuid1),
      Some(uuid2)
    )
    val protoUuids = protobuf.protobuf.UUIDWrapper(
      uuid1.toString,
      Some(
        alloy.protobuf.types.CompactUUID(
          uuid2.value.getMostSignificantBits(),
          uuid2.value.getLeastSignificantBits()
        )
      )
    )
    val protoUuidsBytes = protoUuids.toByteArray
    val uuidsCodec = ProtobufCodec.fromSchema(protobuf.UUIDWrapper.schema)
    val parsedUuids =
      uuidsCodec.unsafeReadBlob(Blob(protoUuidsBytes))

    val uuidsBytes = uuidsCodec.writeBlob(uuids)
    val parsedProtoUuids =
      protobuf.protobuf.UUIDWrapper.parseFrom(uuidsBytes.toArray)

    assertEquals(parsedUuids, uuids)
    assertEquals(parsedProtoUuids, protoUuids)
  }

  test("Timestamps") {
    val timestamp = Timestamp(512, 1024)

    val protoTimestamp = com.google.protobuf.Timestamp
      .newBuilder()
      .setSeconds(512)
      .setNanos(1024)
      .build()
    val protoTimestampBytes = protoTimestamp.toByteArray()
    val timestampCodec = ProtobufCodec.fromSchema(Schema.timestamp)
    val parsedTimestamp =
      timestampCodec.unsafeReadBlob(Blob(protoTimestampBytes))

    val timestampBytes = timestampCodec.writeBlob(timestamp)
    val parsedProtoTimestamp =
      com.google.protobuf.Timestamp.parseFrom(timestampBytes.toArray)

    assertEquals(parsedTimestamp, timestamp)
    assertEquals(parsedProtoTimestamp, protoTimestamp)
  }

  test("Timestamps (epoch seconds)") {
    val timestamp = Timestamp(512, 333 * 1000000)
    val epochMilli = timestamp.epochMilli
    val timestampSchema = smithy4s.schema.Schema.timestamp
      .addHints(alloy.proto.ProtoTimestampFormat.EPOCH_MILLIS.widen)

    val protoTimestamp =
      alloy.protobuf.types.EpochMillisTimestamp(epochMilli)
    val protoTimestampBytes = protoTimestamp.toByteArray
    val timestampCodec = ProtobufCodec.fromSchema(timestampSchema)
    val parsedTimestamp =
      timestampCodec.unsafeReadBlob(Blob(protoTimestampBytes))

    val timestampBytes = timestampCodec.writeBlob(timestamp)
    val parsedProtoTimestamp =
      alloy.protobuf.types.EpochMillisTimestamp
        .parseFrom(timestampBytes.toArray)

    assertEquals(parsedTimestamp, timestamp)
    assertEquals(parsedProtoTimestamp, protoTimestamp)
  }

  test("Documents") {
    import com.google.protobuf._
    import com.google.protobuf.util._
    import Document.syntax._
    val document = obj(
      "null" -> nullDoc,
      "boolean" -> true,
      "number" -> 42.23d,
      "string" -> "John Doe",
      "array" -> array(false, 1, "two"),
      "object" -> obj("nested" -> "Hello")
    )

    val json = """|{
                  |  "null": null,
                  |  "boolean": true,
                  |  "number": 42.23,
                  |  "string": "John Doe",
                  |  "array" : [false, 1, "two"],
                  |  "object": {
                  |    "nested": "Hello"
                  |  }
                  |}
                  |""".stripMargin

    val protoJsonBuilder = Value.newBuilder()
    JsonFormat.parser().ignoringUnknownFields().merge(json, protoJsonBuilder)
    val protoJson = protoJsonBuilder.build()
    val protoJsonBytes = protoJson.toByteArray()
    val documentCodec = ProtobufCodec.fromSchema(Schema.document)
    val parsedDocument = documentCodec.unsafeReadBlob(Blob(protoJsonBytes))

    val documentBytes = documentCodec.writeBlob(document)
    val parsedProtoJson = Value.parseFrom(documentBytes.toArray)

    assertEquals(parsedDocument, document)
    assertEquals(parsedProtoJson, protoJson)
  }

}
