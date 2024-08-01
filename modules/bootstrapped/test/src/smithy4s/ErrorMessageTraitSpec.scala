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
import smithy4s.example._

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

  test(
    "Custom @errorMessage field works for various nullable/default/required combos"
  ) {
    val customMessage = "some custom error message"
    val errorsWithCustomMessage = List(
      ErrorCustomTypeMessage(Some(CustomErrorMessageType(customMessage))),
      ErrorCustomTypeRequiredMessage(CustomErrorMessageType(customMessage)),
      ErrorNullableMessage(Some(Nullable.Value(customMessage))),
      ErrorNullableRequiredMessage(Nullable.Value(customMessage)),
      ErrorNullableCustomTypeMessage(
        Some(Nullable.Value(CustomErrorMessageType(customMessage)))
      ),
      ErrorNullableCustomTypeRequiredMessage(
        Nullable.Value(CustomErrorMessageType(customMessage))
      ),
      ErrorRequiredMessage(customMessage)
    )
    val errorsWithNullMessage = List(
      ErrorCustomTypeMessage(None),
      ErrorNullableMessage(None),
      ErrorNullableMessage(Some(Nullable.Null)),
      ErrorNullableRequiredMessage(Nullable.Null),
      ErrorNullableCustomTypeMessage(None),
      ErrorNullableCustomTypeMessage(Some(Nullable.Null)),
      ErrorNullableCustomTypeRequiredMessage(Nullable.Null),
      ServerErrorCustomMessage(None)
    )

    errorsWithCustomMessage.foreach(e =>
      assertEquals(
        e.getMessage,
        customMessage,
        s"Failed on ${e.getClass.getName}"
      )
    )
    errorsWithNullMessage.foreach(e =>
      assertEquals(e.getMessage, null, s"Failed on ${e.getClass.getName}")
    )
  }

  test("Generated - no message") {
    val e = ClientError(400, "oopsy")

    val expected = "smithy4s.example.ClientError(400, oopsy)"
    expect.eql(e.getMessage, expected)
    expect.eql(e.toString, s"smithy4s.example.ClientError: $expected")
  }

  test("Generated - has message") {
    val e =
      ErrorCustomTypeMessage(Some(CustomErrorMessageType("This is a test.")))

    val expected = "smithy4s.example.ErrorCustomTypeMessage: This is a test."
    expect.eql(e.getMessage, "This is a test.")
    expect.eql(e.toString, expected)
  }

}
