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

import smithy4s.example.Name
import smithy4s.example.ValidatedConstrainedList
import smithy4s.example.ValidatedSetConstrainedMember
import smithy4s.example.ValidatedConstrainedIndexedSeqConstrainedMember
import smithy4s.example.ValidatedConstrainedListRefinedMember
import smithy4s.example.ValidatedConstrainedVectorRefinedConstrainedMember
import smithy4s.example.ValidatedMapConstrainedKey
import smithy4s.example.ValidatedConstrainedMap
import smithy4s.example.ValidatedMapConstrainedValue
import smithy4s.example.ValidatedRefinedListConstrainedMember
import smithy4s.example.AccountId
import smithy4s.example.DeviceId

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

  test("should allow to derive typeclasses in a generic way") {
    trait MyCodec[A] {
      def decode(str: String): Either[String, A]
      def encode(a: A): String
    }

    object MyCodec {
      implicit val stringCodec: MyCodec[String] = new MyCodec[String] {
        def decode(str: String): Either[String, String] = Right(str)
        def encode(a: String): String = a
      }
    }

    def genericCodec[A, B: MyCodec](s: Surjection[B, A]) = new MyCodec[A] {
      def decode(str: String): Either[String, A] =
        implicitly[MyCodec[B]].decode(str).flatMap(s.to)
      def encode(a: A): String = implicitly[MyCodec[B]].encode(s.from(a))
    }

    val accountIdCodec =
      genericCodec[AccountId, String](implicitly[Surjection[String, AccountId]])

    expect.same(accountIdCodec.encode(AccountId.unsafeApply(id1)), id1)
    expect.same(accountIdCodec.decode(id1), Right(AccountId.unsafeApply(id1)))
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

  test("Validated constrained list") {
    expect(ValidatedConstrainedList(List("foo")).isRight)
    expect.same(
      ValidatedConstrainedList(List("foo", "bar")),
      Left("length required to be <= 1, but was 2")
    )
  }

  test("Validated set constrained member") {
    expect(ValidatedSetConstrainedMember(Set("f")).isRight)
    expect.same(
      ValidatedSetConstrainedMember(Set("foo")),
      Left("length required to be <= 2, but was 3")
    )
  }

  test("Validated constrained indexed seq constrained member") {
    expect(
      ValidatedConstrainedIndexedSeqConstrainedMember(IndexedSeq("f")).isRight
    )
    expect.same(
      ValidatedConstrainedIndexedSeqConstrainedMember(IndexedSeq("foo")),
      Left("length required to be <= 2, but was 3")
    )
    expect.same(
      ValidatedConstrainedIndexedSeqConstrainedMember(IndexedSeq("f", "g")),
      Left("length required to be <= 1, but was 2")
    )
    expect.same(
      ValidatedConstrainedIndexedSeqConstrainedMember(IndexedSeq("foo", "h")),
      Left("length required to be <= 1, but was 2")
    )
  }

  test("Validated constrained list refined member") {
    expect(
      ValidatedConstrainedListRefinedMember(
        List(Name(mkName("foo")))
      ).isRight
    )
    expect.same(
      ValidatedConstrainedListRefinedMember(
        List(
          Name(mkName("foo")),
          Name(mkName("bar"))
        )
      ),
      Left("length required to be <= 1, but was 2")
    )
  }

  test("Validated constrained vector refined & constrained member") {

    expect(
      ValidatedConstrainedVectorRefinedConstrainedMember(
        Vector(Name(mkName("fo")))
      ).isRight
    )
    expect.same(
      ValidatedConstrainedVectorRefinedConstrainedMember(
        Vector(
          Name(mkName("fo")),
          Name(mkName("ba"))
        )
      ),
      Left("length required to be <= 1, but was 2")
    )
    expect.same(
      ValidatedConstrainedVectorRefinedConstrainedMember(
        Vector(
          Name(mkName("foo"))
        )
      ),
      Left("length required to be <= 2, but was 3")
    )
  }

  test("Validated refined list constrainer member") {
    expect(ValidatedRefinedListConstrainedMember(mkNel("fo")).isRight)
    expect.same(
      ValidatedRefinedListConstrainedMember(mkNel("fo", "foo")),
      Left("length required to be <= 2, but was 3")
    )
  }

  test("Validated constrained map") {
    expect(ValidatedConstrainedMap(Map("foo" -> 1)).isRight)
    expect.same(
      ValidatedConstrainedMap(Map("foo" -> 1, "bar" -> 2)),
      Left("length required to be <= 1, but was 2")
    )
  }

  test("Validated map constrained key") {
    expect(ValidatedMapConstrainedKey(Map("a" -> 1, "b" -> 2)).isRight)
    expect.same(
      ValidatedMapConstrainedKey(Map("a" -> 1, "bar" -> 2)),
      Left("length required to be <= 2, but was 3")
    )
  }

  test("Validated map constrained value") {
    expect(
      ValidatedMapConstrainedValue(Map("a" -> "123", "b" -> "456")).isRight
    )
    expect.same(
      ValidatedMapConstrainedValue(Map("a" -> "123", "b" -> "4-5-6")),
      Left("String '4-5-6' does not match pattern '^[a-zA-Z0-9]+$'")
    )
  }

  private def mkNel[A](elems: A*) =
    smithy4s.refined.NonEmptyList(elems.toList) match {
      case Left(msg) => fail(msg)
      case Right(v)  => v
    }

  private def mkName(str: String) = smithy4s.refined.Name(str) match {
    case Left(msg) => fail(msg)
    case Right(v)  => v
  }

}
