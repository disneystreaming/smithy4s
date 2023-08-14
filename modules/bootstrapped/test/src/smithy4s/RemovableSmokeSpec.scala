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

class RemovableSmokeSpec() extends FunSuite {

  test(
    "Removable works well with Documents (removed)"
  ) {
    // Here we're testing that `Removable` and `Option` exhibit different behaviours
    // in Document serialisation : `Removable.Removed` is forcefully encoded as `null`,
    // and deserialisation interprets `null` as `Removable.Removed` as opposed to `Removable.Absent`,
    // when the two cannot be distinguished when using Option.
    val patchable = Patchable(1, None, Removable.Removed)
    val result = Document.encode(patchable)
    val expectedDocument =
      Document.obj("a" -> Document.fromInt(1), "c" -> Document.DNull)
    val roundTripped = Document.decode[Patchable](result)
    assertEquals(result, expectedDocument)
    assertEquals(roundTripped, Right(patchable))
  }

  test(
    "Removable works well with Documents (absent)"
  ) {
    val patchable = Patchable(1, None, Removable.Absent)
    val result = Document.encode(patchable)
    val expectedDocument =
      Document.obj("a" -> Document.fromInt(1))
    val roundTripped = Document.decode[Patchable](result)
    assertEquals(result, expectedDocument)
    assertEquals(roundTripped, Right(patchable))
  }

}
