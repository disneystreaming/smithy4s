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
package http

import cats.Show
import cats.data.NonEmptyList
import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import smithy4s.http.PathSegment.GreedySegment
import smithy4s.http.PathSegment.LabelSegment
import smithy4s.http.PathSegment.StaticSegment
import org.scalacheck.Prop._

class MatchPathSpec() extends munit.FunSuite with munit.ScalaCheckSuite {

  def doMatch(segments: List[PathSegment])(
      path: String*
  ): Option[Map[String, String]] =
    matchPath(segments.toList, path.toIndexedSeq)

  implicit def arbNel[T: Arbitrary]: Arbitrary[NonEmptyList[T]] = Arbitrary {
    Gen.resultOf(NonEmptyList.apply[T] _)
  }

  implicit val arbPathSegment: Arbitrary[PathSegment] = Arbitrary {
    Gen.oneOf(
      Gen.resultOf(PathSegment.label(_)),
      Gen.resultOf(PathSegment.greedy(_)),
      Gen.resultOf(PathSegment.static(_))
    )
  }

  val genLabelOrStatic: Gen[PathSegment] = Gen.oneOf(
    Gen.resultOf(PathSegment.label(_)),
    Gen.resultOf(PathSegment.static(_))
  )

  implicit val showPathSegment: Show[PathSegment] = Show.fromToString

  private val renderExampleSegment: PathSegment => String = {
    case LabelSegment(_)      => "label-example"
    case StaticSegment(value) => value
    case GreedySegment(_) =>
      "greedy/example"
  }

  property("Doesn't throw on empty path") {
    forAll { (segments: NonEmptyList[PathSegment]) =>
      expect.eql(doMatch(segments.toList)(), None)
    }
  }

  property("Doesn't throw on partially matching paths") {
    val gen = Gen.zip(
      Gen.listOf(genLabelOrStatic),
      Arbitrary.arbitrary[NonEmptyList[PathSegment]]
    )
    forAll(gen) { case (prefix, segments) =>
      val fullPath = prefix ::: segments.toList
      val actual = prefix.map(renderExampleSegment)
      expect.eql(doMatch(fullPath)(actual: _*), None)
    }
  }

  test("Allows for greedy labels") {
    // /hello/{foo*}
    val path: List[PathSegment] =
      List(PathSegment.static("hello"), PathSegment.greedy("foo"))
    val expected = Map("foo" -> "foo/bar/baz")
    val result = doMatch(path)("hello", "foo", "bar", "baz")

    expect.eql(result, Some(expected))
  }

  test("Match several segments") {
    // /{foo}/bar/{baz}
    val path: List[PathSegment] =
      List(
        PathSegment.label("foo"),
        PathSegment.static("bar"),
        PathSegment.label("baz")
      )
    val expected = Map("foo" -> "a", "baz" -> "b")
    val result = doMatch(path)("a", "bar", "b")

    expect.eql(result, Some(expected))
  }

  test("Decodes URLencoded characters in path segments") {
    // /{foo}/hello
    val path: List[PathSegment] =
      List(
        PathSegment.label("foo"),
        PathSegment.static("hello")
      )

    val result = doMatch(path)("hello world", "hello")

    val expected = Map("foo" -> "hello world")
    expect.eql(result, Some(expected))
  }
  test("Decodes URLencoded characters in greedy segments") {
    // /hello/{foo*}
    val path: List[PathSegment] =
      List(
        PathSegment.static("hello"),
        PathSegment.greedy("foo")
      )

    val result = doMatch(path)("hello", "hello world", "goodbye world")

    val expected = Map("foo" -> "hello world/goodbye world")
    expect.eql(result, Some(expected))
  }

}
