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

package smithy4s.http

import cats.Show
import cats.data.NonEmptyList
import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import weaver.SimpleMutableIOSuite
import weaver.scalacheck.Checkers
import smithy4s.http.PathSegment.GreedySegment
import smithy4s.http.PathSegment.LabelSegment
import smithy4s.http.PathSegment.StaticSegment

object MatchPathSpec extends SimpleMutableIOSuite with Checkers {

  def doMatch(segments: List[PathSegment])(
      string: String
  ): Option[Map[String, String]] =
    matchPath(segments.toList, matchPath.make(string))

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

  test("Doesn't throw on empty path") {
    forall { (segments: NonEmptyList[PathSegment]) =>
      expect.eql(doMatch(segments.toList)("/"), None)
    }
  }

  test("Doesn't throw on partially matching paths") {
    forall(Gen.listOf(genLabelOrStatic)) { prefix =>
      forall { segments: NonEmptyList[PathSegment] =>
        val fullPath = prefix ::: segments.toList
        val actual = prefix.map(renderExampleSegment).mkString("/")
        expect.eql(doMatch(fullPath)(actual), None)
      }
    }
  }

  pureTest("Is lenient with regards to trailing slashes") {
    // /{foo}
    val path: List[PathSegment] =
      List(PathSegment.label("foo"))
    val expected = Map("foo" -> "bar")
    val result = doMatch(path)("/bar")
    val result2 = doMatch(path)("/bar/")

    expect.eql(result, Some(expected)) &&
    expect.eql(result2, Some(expected))
  }

  pureTest("Allows for greedy labels") {
    // /{foo}
    val path: List[PathSegment] =
      List(PathSegment.static("hello"), PathSegment.greedy("foo"))
    val expected = Map("foo" -> "foo/bar/baz")
    val result = doMatch(path)("/hello/foo/bar/baz")
    val result2 = doMatch(path)("/hello/foo/bar/baz/")

    expect.eql(result, Some(expected)) &&
    expect.eql(result2, Some(expected))
  }

  pureTest("Match several segments") {
    // /{foo}
    val path: List[PathSegment] =
      List(
        PathSegment.label("foo"),
        PathSegment.static("bar"),
        PathSegment.label("baz")
      )
    val expected = Map("foo" -> "a", "baz" -> "b")
    val result = doMatch(path)("/a/bar/b/")
    val result2 = doMatch(path)("/a/baz/b")

    expect.eql(result, Some(expected)) &&
    expect.eql(result2, None)
  }

}
