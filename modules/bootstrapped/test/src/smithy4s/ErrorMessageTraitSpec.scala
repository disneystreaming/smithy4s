/*
 *  Copyright 2021-2023 Disney Streaming
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
import smithy4s.example.ClientError
import smithy4s.example.ServerErrorCustomMessage

class ErrorMessageTraitSpec extends FunSuite {

  test(
    "Error type with a custom @errorMessage field uses that field as the error message"
  ) {
    val e = ServerErrorCustomMessage(Some("error message"))

    expect.eql(e.getMessage, "error message")
    expect.eql(
      e.toString,
      "smithy4s.example.ServerErrorCustomMessage: error message"
    )
  }

  test("Generated getMessage") {
    val e = ClientError(400, "oopsy")

    val expected = "smithy4s.example.ClientError(400, oopsy)"
    expect.eql(e.getMessage, null)
    expect.eql(e.toString, expected)
  }

}
