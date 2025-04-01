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
import scalapb.GeneratedMessage
import smithy4s.Blob
import smithy4s.example.protobuf
import smithy4s.schema.Schema
import smithy4s.ShapeId

class CodecTests() extends FunSuite {

  test("Integers") {
    checkFull(
      protobuf.Integers(1, 1, 1, 1, 1),
      protobuf.protobuf.Integers(1, 1, 1, 1, 1)
    )
  }

  test("Integers (zeros)") {
    checkFull(
      protobuf.Integers(0, 0, 0, 0, 0),
      protobuf.protobuf.Integers(0, 0, 0, 0, 0)
    )
  }

  test("Longs") {
    checkFull(
      protobuf.Longs(1, 1, 1, 1, 1),
      protobuf.protobuf.Longs(1, 1, 1, 1, 1)
    )
  }

  test("Longs (zeros)") {
    checkFull(
      protobuf.Longs(0, 0, 0, 0, 0),
      protobuf.protobuf.Longs(0, 0, 0, 0, 0)
    )
  }

  test("Longs (negatives)") {
    checkFull(
      protobuf.Longs(-1, -1, -1, -1, -1),
      protobuf.protobuf.Longs(-1, -1, -1, -1, -1)
    )
  }

  test("Other scalars") {
    checkFull(
      protobuf.OtherScalars(true, 1, 1, 1, 1),
      protobuf.protobuf.OtherScalars(true, 1, 1, 1, 1)
    )
  }

  test("Other scalars (zeros)") {
    checkFull(
      protobuf.OtherScalars(false, 0, 0, 0, 0),
      protobuf.protobuf.OtherScalars(false, 0, 0, 0, 0)
    )
  }

  test("Optional scalars") {
    checkFull(
      protobuf.WrappedScalars(int = Some(3), bool = Some(true)),
      protobuf.protobuf.WrappedScalars(int = Some(3), bool = Some(true))
    )
  }

  test("Optional scalars (zeros)") {
    checkFull(
      protobuf.WrappedScalars(int = Some(0), bool = Some(false)),
      protobuf.protobuf.WrappedScalars(int = Some(0), bool = Some(false))
    )
  }

  test("Optional scalars (nones)") {
    checkFull(
      protobuf.WrappedScalars(int = None, bool = None),
      protobuf.protobuf.WrappedScalars(int = None, bool = None)
    )
  }

  test("String") {
    checkFull(
      protobuf.StringWrapper("aaa"),
      protobuf.protobuf.StringWrapper("aaa")
    )
  }

  test("String (zero)") {
    checkFull(
      protobuf.StringWrapper(""),
      protobuf.protobuf.StringWrapper("")
    )
  }

  test("BigDecimal") {
    checkFull(
      protobuf.BigDecimalWrapper(BigDecimal("123")),
      protobuf.protobuf.BigDecimalWrapper("123")
    )
  }

  test("BigDecimal (zeros)") {
    checkFull(
      protobuf.BigDecimalWrapper(BigDecimal("0")),
      protobuf.protobuf.BigDecimalWrapper("")
    )
  }

  test("UUID empty") {
    checkFull(
      protobuf.UUIDWrapper(None, None),
      protobuf.protobuf.UUIDWrapper("", None)
    )
  }

  test("Scalar list") {
    checkFull(
      protobuf.IntListWrapper(List(1, 2, 3)),
      protobuf.protobuf.IntListWrapper(List(1, 2, 3))
    )
  }

  test("Scalar list (empty)") {
    checkFull(
      protobuf.IntListWrapper(Nil),
      protobuf.protobuf.IntListWrapper(Nil)
    )
  }

  test("Non-scalar list") {
    checkFull(
      protobuf.StringListWrapper(List("1", "2", "3"), List("4", "5", "6")),
      protobuf.protobuf.StringListWrapper(
        List("1", "2", "3"),
        Some(protobuf.protobuf.WrappedStringList(List("4", "5", "6")))
      )
    )
  }

  test("Non-scalar list (empty)") {
    checkFull(
      protobuf.StringListWrapper(List.empty, List.empty),
      protobuf.protobuf.StringListWrapper(
        List.empty,
        Some(protobuf.protobuf.WrappedStringList(List.empty))
      )
    )
  }

  test("Maps") {
    checkFull(
      protobuf.StringMapWrapper(Map("1" -> 1, "2" -> 2, "3" -> 3)),
      protobuf.protobuf.StringMapWrapper(Map("1" -> 1, "2" -> 2, "3" -> 3))
    )
  }

  test("Maps (empty)") {
    checkFull(
      protobuf.StringMapWrapper(Map.empty),
      protobuf.protobuf.StringMapWrapper(Map.empty)
    )
  }

  test("Newtype lists") {
    // TODO : discuss (may need smithy-translate amendment)
    // We purposefully avoid comparing against the Scalapb-generated `MyIntListWrapper` as
    // the creation of wrapper types for `MyInt` is likely undesirable.
    checkFull(
      protobuf.MyIntListWrapper(List(1, 2, 3).map(protobuf.MyInt(_))),
      protobuf.protobuf.IntListWrapper(List(1, 2, 3))
    )
  }

  test("MessageWrapper") {
    checkFull(
      protobuf.MessageWrapper(protobuf.Integers(1, 1, 1, 1, 1)),
      protobuf.protobuf.MessageWrapper(
        Some(protobuf.protobuf.Integers(1, 1, 1, 1, 1))
      )
    )
  }

  test(
    "MessageWrapper (required message fields fail from an absence of value)"
  ) {
    val proto = protobuf.protobuf.MessageWrapper(None)

    val codec = ProtobufCodec[protobuf.MessageWrapper]
    val protoBytes = Blob(proto.toByteArray)
    val smithyParsed = codec.readBlob(protoBytes)
    val expected = Left(
      ProtobufReadError.MissingRequiredField(
        ShapeId("smithy4s.example.protobuf", "MessageWrapper"),
        "message",
        1
      )
    )
    assertEquals(smithyParsed, expected)
  }

  test("MessageWrapper (zeros)") {
    checkFull(
      protobuf.MessageWrapper(protobuf.Integers(0, 0, 0, 0, 0)),
      protobuf.protobuf.MessageWrapper(
        Some(protobuf.protobuf.Integers(0, 0, 0, 0, 0))
      )
    )
  }

  test("OptionalMessageWrapper") {
    checkFull(
      protobuf.OptionalMessageWrapper(Some(protobuf.Integers(1, 1, 1, 1, 1))),
      protobuf.protobuf.OptionalMessageWrapper(
        Some(protobuf.protobuf.Integers(1, 1, 1, 1, 1))
      )
    )
  }

  test("OptionalMessageWrapper (none)") {
    checkFull(
      protobuf.OptionalMessageWrapper(None),
      protobuf.protobuf.OptionalMessageWrapper(None)
    )
  }

  test("Unions (non-inlined)") {
    checkFull(
      protobuf.UnionWrapper(Some(protobuf.MyUnion.int(1))),
      protobuf.protobuf.UnionWrapper(
        Some(
          protobuf.protobuf.MyUnion(
            protobuf.protobuf.MyUnion.Definition.Int(1)
          )
        )
      )
    )
  }

  test("Unions (zero)") {
    checkFull(
      protobuf.UnionWrapper(Some(protobuf.MyUnion.int(0))),
      protobuf.protobuf.UnionWrapper(
        Some(
          protobuf.protobuf.MyUnion(
            protobuf.protobuf.MyUnion.Definition.Int(0)
          )
        )
      )
    )
  }

  test("Unions (list)") {
    checkFull(
      protobuf.UnionWrapper(
        Some(protobuf.MyUnion.list(List(1, 2, 3).map(protobuf.MyInt(_))))
      ),
      protobuf.protobuf.UnionWrapper(
        Some(
          protobuf.protobuf.MyUnion(
            protobuf.protobuf.MyUnion.Definition
              .List(protobuf.protobuf.MyIntList(Seq(1, 2, 3)))
          )
        )
      )
    )
  }

  test("Unions (map)") {
    checkFull(
      protobuf.UnionWrapper(
        Some(protobuf.MyUnion.map(Map("a" -> 1, "b" -> 2)))
      ),
      protobuf.protobuf.UnionWrapper(
        Some(
          protobuf.protobuf.MyUnion(
            protobuf.protobuf.MyUnion.Definition
              .Map(protobuf.protobuf.StringMap(Map("a" -> 1, "b" -> 2)))
          )
        )
      )
    )
  }

  test("Unions (inlined)") {
    checkFull(
      protobuf.InlinedUnionWrapper(Some(protobuf.MyInlinedUnion.int(1))),
      protobuf.protobuf.InlinedUnionWrapper(
        protobuf.protobuf.InlinedUnionWrapper.MyInlinedUnion.Int(1)
      )
    )
  }

  test("Recursive") {
    checkFull(
      protobuf.Recursive(Some(protobuf.Recursive(None))),
      protobuf.protobuf.Recursive(Some(protobuf.protobuf.Recursive(None)))
    )
  }

  test("Recursive (empty)") {
    checkFull(
      protobuf.Recursive(None),
      protobuf.protobuf.Recursive(None)
    )
  }

  test("Enums") {
    checkFull(
      protobuf.Enums(
        protobuf.ClosedString.FOO,
        protobuf.OpenString.FOO,
        protobuf.ClosedInt.FOO,
        protobuf.OpenInt.FOO
      ),
      protobuf.protobuf.Enums(
        protobuf.protobuf.ClosedString.CLOSEDSTRING_FOO,
        "FOO",
        protobuf.protobuf.ClosedInt.CLOSEDINT_FOO,
        0
      )
    )
  }

  test("Enums (bis)") {
    checkFull(
      protobuf.Enums(
        protobuf.ClosedString.BAR,
        protobuf.OpenString.$Unknown("BAZ"),
        protobuf.ClosedInt.BAR,
        protobuf.OpenInt.$Unknown(42)
      ),
      protobuf.protobuf.Enums(
        protobuf.protobuf.ClosedString.CLOSEDSTRING_BAR,
        "BAZ",
        protobuf.protobuf.ClosedInt.CLOSEDINT_BAR,
        42
      )
    )
  }

  test("Refinement") {
    val range: smithy4s.Hint =
      smithy.api.Range(Some(BigDecimal(1.0)), Some(BigDecimal(10.0)))
    val codec = ProtobufCodec.fromSchema(protobuf.RefinedIntWrapped.schema)
    val parsedRefinedInt = codec.readBlob(
      Blob(protobuf.protobuf.RefinedIntWrapped(0).toByteArray)
    )

    parsedRefinedInt match {
      case Left(e: ProtobufReadError.ViolatedConstraint) if e.hint == range =>
        ()
      case other => fail(clue(other.toString))
    }

  }

  test("Custom indexes") {
    // Using checkMutual as the ordering of serialised bytes may be different between ScalaPB and smithy4s-protobuf.
    // As long as they can mutually understand each other, we're good.
    checkMutual(
      protobuf.StructureWithCustomIndexes(
        c = 3,
        b = 2,
        a = Some(1),
        d = Some(protobuf.UnionWithCustomIndexes.b(4))
      ),
      protobuf.protobuf.StructureWithCustomIndexes(
        a = 1,
        b = 2,
        c = 3,
        d = Some(protobuf.protobuf.UnionWithCustomIndexes().withB(4))
      )
    )
  }

  def checkMutual[SmithyType: Schema, ProtoType <: GeneratedMessage](
      smithy: SmithyType,
      proto: ProtoType
  )(implicit loc: Location) = {
    val codec = ProtobufCodec[SmithyType]
    val smithyBytes = codec.writeBlob(smithy)
    val protoBytes = Blob(proto.toByteArray)
    val smithyParsed = codec.unsafeReadBlob(protoBytes)
    val protoParsed =
      proto.companion.parseFrom(smithyBytes.toArray).asInstanceOf[ProtoType]
    assertEquals(smithyParsed, smithy)
    assertEquals(protoParsed, proto)
  }
  def checkFull[SmithyType: Schema, ProtoType <: GeneratedMessage](
      smithy: SmithyType,
      proto: ProtoType
  )(implicit loc: Location) = {
    checkEncode(smithy, proto)
    checkRoundTrip(smithy)
  }

  def checkEncode[SmithyType: Schema, ProtoType <: GeneratedMessage](
      smithy: SmithyType,
      proto: ProtoType
  )(implicit loc: Location) = {

    val smithy4sBlob =
      ProtobufCodec[SmithyType].writeBlob(smithy).toArray.toVector
    val scalapbBlob = proto.toByteArray.toVector

    assertEquals(smithy4sBlob, scalapbBlob)
  }

  def checkRoundTrip[SmithyType: Schema](
      initial: SmithyType
  )(implicit loc: Location) = {
    val blob =
      ProtobufCodec[SmithyType].writeBlob(initial)
    val decoded = ProtobufCodec[SmithyType].unsafeReadBlob(blob)

    assertEquals(decoded, initial)
  }

}
