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

import smithy4s.refined.NonEmptyList
import smithy4s.example.Name
import smithy4s.example.ValidatedConstrainedList
import smithy4s.example.ValidatedListConstrainedMember
import smithy4s.example.ValidatedConstrainedListConstrainedMember
import smithy4s.example.ValidatedConstrainedListRefinedMember
import smithy4s.example.ValidatedConstrainedListRefinedConstrainedMember
import smithy4s.example.ValidatedMapConstrainedKey
import smithy4s.example.ValidatedConstrainedMap
import smithy4s.example.ValidatedMapConstrainedValue
import smithy4s.example.ValidatedRefinedListConstrainedMember
import smithy4s.example.AccountId
import smithy4s.example.DeviceId

import munit.Assertions
import cats.data.Validated.Valid
import smithy.api.Length
import smithy4s.example.NonEmptyListFormat

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

  test("Validated constrained list") {
    expect(ValidatedConstrainedList(List("foo")).isRight)
    expect(ValidatedConstrainedList(List("foo", "bar")).isLeft)
  }

  test("Validated list constrained member") {
    expect(ValidatedListConstrainedMember(List("f")).isRight)
    expect(ValidatedListConstrainedMember(List("foo")).isLeft)
  }

  test("Validated constrained list constrained member") {
    expect(ValidatedConstrainedListConstrainedMember(List("f")).isRight)
    expect(ValidatedConstrainedListConstrainedMember(List("foo")).isLeft)
    expect(ValidatedConstrainedListConstrainedMember(List("f", "g")).isLeft)
    expect(ValidatedConstrainedListConstrainedMember(List("foo", "h")).isLeft)
  }

  test("Validated constrained list refined member") {
    expect(
      ValidatedConstrainedListRefinedMember(
        List(Name(mkName("foo")))
      ).isRight
    )
    expect(
      ValidatedConstrainedListRefinedMember(
        List(
          Name(mkName("foo")),
          Name(mkName("bar"))
        )
      ).isLeft
    )
  }

  test("Validated constrained list refined & constrained member") {

    expect(
      ValidatedConstrainedListRefinedConstrainedMember(
        List(Name(mkName("fo")))
      ).isRight
    )
    expect(
      ValidatedConstrainedListRefinedConstrainedMember(
        List(
          Name(mkName("fo")),
          Name(mkName("ba"))
        )
      ).isLeft
    )
    expect(
      ValidatedConstrainedListRefinedConstrainedMember(
        List(
          Name(mkName("foo"))
        )
      ).isLeft
    )
  }

  test("Validated refined list constrainer member") {
    expect(ValidatedRefinedListConstrainedMember(mkNel("fo")).isRight)
    expect(ValidatedRefinedListConstrainedMember(mkNel("fo", "foo")).isLeft)
  }

  test("Validated constrained map") {
    expect(ValidatedConstrainedMap(Map("foo" -> 1)).isRight)
    expect(ValidatedConstrainedMap(Map("foo" -> 1, "bar" -> 2)).isLeft)
  }

  test("Validated map constrained key") {
    expect(ValidatedMapConstrainedKey(Map("a" -> 1, "b" -> 2)).isRight)
    expect(ValidatedMapConstrainedKey(Map("a" -> 1, "bar" -> 2)).isLeft)
  }

  test("Validated map constrained value") {
    expect(ValidatedMapConstrainedValue(Map("a" -> 1, "b" -> 2)).isRight)
    expect(ValidatedMapConstrainedValue(Map("a" -> 1, "b" -> 3)).isLeft)
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
