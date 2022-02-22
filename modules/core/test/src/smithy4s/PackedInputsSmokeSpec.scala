/*
 *  Copyright 2021 Disney Streaming
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

import weaver._
import cats.Id
import smithy4s.example.{PackedInputsService, PackedInput}

object PackedInputsSmokeSpec extends FunSuite {

  test("Methods with packed inputs have a single case-class parameter") {
    val service: PackedInputsService[Id] = new PackedInputsService[Id] {
      def packedInputOperation(input: PackedInput): Unit = ()
    }
    expect.same(service.packedInputOperation(PackedInput(key = "foo")), ())
  }

}
