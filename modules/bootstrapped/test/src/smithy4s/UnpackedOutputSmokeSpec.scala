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

package smithy4s

import cats.Id
import smithy4s.example.UnpackedItem
import smithy4s.example.UnpackedOutputService
import munit._

class UnpackedOutputSmokeSpec() extends FunSuite {

  private val service: UnpackedOutputService[Id] = new UnpackedOutputService[Id] {
    def getRequiredItem(): UnpackedItem = UnpackedItem("required")
    def getOptionalItem(): Option[UnpackedItem] = Some(UnpackedItem("optional"))
  }

  test("@unpackedOutput on operation with required field returns the inner type") {
    expect.same(service.getRequiredItem(), UnpackedItem("required"))
  }

  test("@unpackedOutput on operation with optional field returns Option of the inner type") {
    expect.same(service.getOptionalItem(), Some(UnpackedItem("optional")))
  }

}
