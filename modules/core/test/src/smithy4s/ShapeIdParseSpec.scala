/*
 *  Copyright 2021-2022 Disney Streaming
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

package smithy4s
import munit._

class ShapeIdParseSpec extends FunSuite {
  test("ShapeId can be parsed from valid shape IDs") {
    val shapeId = ShapeId.parse("smithy4s#ShapeId")
    expect.same(shapeId, Some(ShapeId("smithy4s", "ShapeId")))
  }

  test("ShapeId doesn't parse if no hash is present") {
    val shapeId = ShapeId.parse("smithy4sShapeId")
    expect(shapeId.isEmpty)
  }

  test("ShapeId doesn't parse if there's nothing before the hash") {
    val shapeId = ShapeId.parse("#ShapeId")
    expect(shapeId.isEmpty)
  }

  test("ShapeId doesn't parse if there's nothing after the hash") {
    val shapeId = ShapeId.parse("smithy4s#")
    expect(shapeId.isEmpty)
  }

  test("ShapeId doesn't parse if multiple hashes are present") {
    val shapeId = ShapeId.parse("smithy4s#Shape#Id")
    expect(shapeId.isEmpty)
  }

  test("Member parses when valid") {
    val memberResult = ShapeId.Member.parse("smithy4s#ShapeId$member")
    expect.same(memberResult, Some(ShapeId("smithy4s", "ShapeId").withMember("member")))
  }

  test("Member parse fails when it's just a shapeId") {
    val memberResult = ShapeId.Member.parse("smithy4s#ShapeId")
    expect(memberResult.isEmpty)
  }

  test("Member parse fails when there's more than one $") {
    val memberResult = ShapeId.Member.parse("smithy4s#ShapeId$member$member")
    expect(memberResult.isEmpty)
  }
}
