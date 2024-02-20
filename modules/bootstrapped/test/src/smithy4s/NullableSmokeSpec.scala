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
import smithy4s.example._

class NullableSmokeSpec() extends FunSuite {

  // Here we're testing that `Nullable` overrides the standard null processing: `Nullable.Null`
  // is forcefully encoded as `null`, and deserialisation interprets `null` as `Nullable.Null`
  // rather than `None`.
  test(
    "Nullable field serialises and deserialises explicit null"
  ) {
    val patchable = Patchable(allowExplicitNull = Some(Nullable.Null))
    val result = Document.encode(patchable)
    val expectedDocument =
      Document.obj("allowExplicitNull" -> Document.DNull)
    val roundTripped = Document.decode[Patchable](result)
    assertEquals(result, expectedDocument)
    assertEquals(roundTripped, Right(patchable))
  }

  test(
    "Nullable field serialises and deserialises absence of a value"
  ) {
    val patchable = Patchable(allowExplicitNull = None)
    val result = Document.encode(patchable)
    val expectedDocument = Document.obj()
    val roundTripped = Document.decode[Patchable](result)
    assertEquals(result, expectedDocument)
    assertEquals(roundTripped, Right(patchable))
  }

  test(
    "Nullable field serialises and deserialises concrete value"
  ) {
    val patchable = Patchable(allowExplicitNull = Some(Nullable.Value(5)))
    val result = Document.encode(patchable)
    val expectedDocument =
      Document.obj("allowExplicitNull" -> Document.DNumber(5))
    val roundTripped = Document.decode[Patchable](result)
    assertEquals(result, expectedDocument)
    assertEquals(roundTripped, Right(patchable))
  }

  // Edge case handling to ensure combinations with default and/or required behave as expected
  test("Defaults are encoded as expected") {
    val patchable = PatchableEdgeCases(required = Nullable.Value(5))
    val explicitPatchable = PatchableEdgeCases(
      required = Nullable.Value(5),
      defaultValue = Nullable.Value(5),
      defaultNull = Nullable.Null,
      requiredDefaultValue = Nullable.Value(3),
      requiredDefaultNull = Nullable.Null
    )
    val result = Document.encode(patchable)
    val expectedDocument = Document.obj(
      "required" -> Document.DNumber(5),
      "requiredDefaultValue" -> Document.DNumber(3),
      "requiredDefaultNull" -> Document.DNull
    )
    val roundTripped = Document.decode[PatchableEdgeCases](result)
    assertEquals(patchable, explicitPatchable)
    assertEquals(result, expectedDocument)
    assertEquals(roundTripped, Right(patchable))
  }

  test("Required field can be encoded and decoded as null") {
    val patchable = PatchableEdgeCases(required = Nullable.Null)
    val result = Document.encode(patchable)
    val expectedDocument = Document.obj(
      "required" -> Document.DNull,
      "requiredDefaultValue" -> Document.DNumber(3),
      "requiredDefaultNull" -> Document.DNull
    )
    val roundTripped = Document.decode[PatchableEdgeCases](result)
    assertEquals(result, expectedDocument)
    assertEquals(roundTripped, Right(patchable))
  }

  test("Defaults can be overridden") {
    val patchable = PatchableEdgeCases(
      required = Nullable.Value(1),
      requiredDefaultValue = Nullable.Null,
      requiredDefaultNull = Nullable.Value(2),
      defaultValue = Nullable.Null,
      defaultNull = Nullable.Value(3)
    )
    val result = Document.encode(patchable)
    val expectedDocument = Document.obj(
      "required" -> Document.DNumber(1),
      "requiredDefaultValue" -> Document.DNull,
      "requiredDefaultNull" -> Document.DNumber(2),
      "defaultValue" -> Document.DNull,
      "defaultNull" -> Document.DNumber(3)
    )
    val roundTripped = Document.decode[PatchableEdgeCases](result)
    assertEquals(result, expectedDocument)
    assertEquals(roundTripped, Right(patchable))
  }

}
