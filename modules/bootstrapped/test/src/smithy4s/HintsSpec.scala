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

import smithy.api.HttpHeader
import smithy.api.HttpLabel
import munit._
import smithy.api.Readonly
import smithy.api.Required
import smithy.api.JsonName
import smithy.api.Documentation
import smithy.api.Tags
// import smithy.api.JsonName
// import smithy.api.Documentation
// import smithy.api.Tags

class HintsSpec() extends FunSuite {
  test("hints work as expected with newtypes") {
    val hints = Hints(HttpHeader("X-Foobar"))
    expect(hints.get(HttpHeader) == Some(HttpHeader("X-Foobar")))
  }

  test("hints work as expected with newtypes (using implicits)") {
    val hints = Hints(HttpHeader("X-Foobar"))
    expect.same(hints.get[HttpHeader], Some(HttpHeader("X-Foobar")))
  }

  test("hints can be stored as member hints") {
    val hints = Hints(HttpLabel()).addMemberHints(HttpHeader("X-Foobar"))
    // Member and target hints are both looked at when searching for a hint.
    expect.same(hints.get(HttpHeader), Some(HttpHeader("X-Foobar")))
    expect.same(hints.get(HttpLabel), Some(HttpLabel()))
  }

  test("Member hints are stored separately from target hints") {
    val hints =
      Hints(HttpHeader("X-Target")).addMemberHints(HttpHeader("X-Member"))
    expect.same(hints.memberHints, Hints.member(HttpHeader("X-Member")))
    expect.same(hints.targetHints, Hints(HttpHeader("X-Target")))
  }

  test(
    "hints stored as member hints have precedence over the ones stored as target hints"
  ) {
    val hints =
      Hints.empty
        .addMemberHints(HttpHeader("X-Member"))
        .addTargetHints(HttpHeader("X-Foobar"))
    expect.same(hints.get(HttpHeader), Some(HttpHeader("X-Member")))
  }

  test("Hints concatenation respect hint level") {
    val concat = Hints.member(HttpHeader("X-Member")) ++ Hints(HttpLabel())
    expect.same(concat.memberHints, Hints.member(HttpHeader("X-Member")))
    expect.same(concat.targetHints, Hints(HttpLabel()))
  }

  test("Hints#add adds to the member layer") {
    val concat = Hints.empty.add(HttpHeader("X-Member"))
    expect.same(concat, Hints.member(HttpHeader("X-Member")))
  }

  test("Hints#addTargetHints adds to the target layer") {
    val concat = Hints.empty.addTargetHints(HttpHeader("X-Member"))
    expect.same(concat, Hints(HttpHeader("X-Member")))
  }

  test("Hints#expand allows to derive a hint from another hint") {
    val expanded0 =
      Hints.empty.expand((_: HttpLabel) => Readonly())
    val expanded1 =
      Hints(HttpLabel()).expand((_: HttpLabel) => Readonly())
    assert(!expanded0.has(Readonly))
    assert(expanded1.has(Readonly))
  }

  test("Hints#concat preserves laziness: two lazies") {
    val (lazyA, evaledA) = makeLazyHints {
      Hints(Required())
    }

    val (lazyB, evaledB) = makeLazyHints {
      Hints(HttpLabel())
    }

    val result = lazyA ++ lazyB

    assert(!evaledA())
    assert(!evaledB())
    assertEquals(
      Hints(Required(), HttpLabel()),
      result
    )
  }

  test("Hints#concat preserves laziness: lazy LHS") {
    val (lazyA, evaledA) = makeLazyHints {
      Hints(Required())
    }

    val rhs = {
      Hints(HttpLabel())
    }

    val result = lazyA ++ rhs

    assert(!evaledA())
    assertEquals(
      Hints(Required(), HttpLabel()),
      result
    )
  }

  test("Hints#concat preserves laziness: lazy RHS") {
    val lhs = {
      Hints(Required())
    }

    val (lazyB, evaledB) = makeLazyHints {
      Hints(HttpLabel())
    }

    val result = lhs ++ lazyB

    assert(!evaledB())
    assertEquals(
      Hints(Required(), HttpLabel()),
      result
    )
  }

  test("Hints#concat: both sides eager") {
    val lhs = {
      Hints(Required())
    }

    val rhs = {
      Hints(HttpLabel())
    }

    val result = lhs ++ rhs

    assertEquals(
      Hints(Required(), HttpLabel()),
      result
    )
  }

  test("Hints can be built from tuples of strings/documents") {
    import Document.syntax._
    lazy val hints = Hints.dynamic(
      "smithy.api#jsonName" -> "foo",
      "smithy.api#documentation" -> "hello",
      "smithy.api#tags" -> array("one", "two", "three")
    )
    assertEquals(hints.get(JsonName), Some(JsonName("foo")))
    assertEquals(hints.get(Documentation), Some(Documentation("hello")))
    assertEquals(hints.get(Tags), Some(Tags(List("one", "two", "three"))))
  }

  private def makeLazyHints(hints: => Hints): (Hints, () => Boolean) = {
    var evaled = false

    val result = {
      evaled = true
      hints
    }.lazily

    (result, () => evaled)
  }
}
