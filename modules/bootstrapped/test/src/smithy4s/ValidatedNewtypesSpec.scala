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

package smithy4s

import smithy4s.schema.Schema.string
import munit.Assertions

class ValidatedNewtypesSpec() extends munit.FunSuite {
  val id1 = "id1"
  val id2 = "id2"

  test("Validated newtypes are consistent") {
    expect.same(AccountId.unsafeApply(id1).value, id1)
    expect.different(
      AccountId.unsafeApply(id1).value,
      AccountId.unsafeApply(id2).value
    )
    expect.different(
      implicitly[ShapeTag[AccountId]],
      implicitly[ShapeTag[DeviceId]]
    )
    expect.same(AccountId.unapply(AccountId.unsafeApply(id1)), Some(id1))
  }

  test("Newtypes have well defined unapply") {
    val aid = AccountId.unsafeApply(id1)
    aid match {
      case AccountId(id) => expect(id == id1)
    }
  }

  test("Validated newtypes unsafeApply throws exception") {
    val e = Assertions.intercept[IllegalArgumentException] {
      AccountId.unsafeApply("!^%&")
    }

    expect.same(
      e.getMessage(),
      "String '!^%&' does not match pattern '[a-zA-Z0-9]+'"
    )
  }

  type DeviceId = DeviceId.Type
  object DeviceId extends ValidatedNewtype[String] {

    val id: ShapeId = ShapeId("foo", "DeviceId")
    val hints: Hints = Hints.empty

    val underlyingSchema: Schema[String] = string
      .withId(id)
      .addHints(hints)
      .validated(smithy.api.Length(min = Some(1L), max = None))

    val validator: Validator[String, DeviceId] = Validator
      .of[String, DeviceId](
        Bijection[String, DeviceId](_.asInstanceOf[DeviceId], value(_))
      )
      .validating(smithy.api.Length(min = Some(1L), max = None))

    implicit val schema: Schema[DeviceId] =
      validator.toSchema(underlyingSchema)

    @inline def apply(a: String): Either[String, DeviceId] =
      validator.validate(a)

  }

  type AccountId = AccountId.Type

  object AccountId extends ValidatedNewtype[String] {
    def id: smithy4s.ShapeId = ShapeId("foo", "AccountId")
    val hints: Hints = Hints.empty

    val underlyingSchema: Schema[String] = string
      .withId(id)
      .addHints(hints)
      .validated(smithy.api.Length(min = Some(1L), max = None))
      .validated(smithy.api.Pattern("[a-zA-Z0-9]+"))

    val validator: Validator[String, AccountId] = Validator
      .of[String, AccountId](
        Bijection[String, AccountId](_.asInstanceOf[AccountId], value(_))
      )
      .validating(smithy.api.Length(min = Some(1L), max = None))
      .alsoValidating(smithy.api.Pattern("[a-zA-Z0-9]+"))

    implicit val schema: Schema[AccountId] =
      validator.toSchema(underlyingSchema)

    @inline def apply(a: String): Either[String, AccountId] =
      validator.validate(a)

  }

}
