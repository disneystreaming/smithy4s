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

package smithy4s.http

import cats.effect._
import cats.syntax.all._
import org.scalacheck.Gen
import smithy.api.{Http, NonEmptyString}
import smithy4s._
import smithy4s.http.HttpEndpoint._
import weaver._
import weaver.scalacheck._

import scala.util._

/**
 * The following algorithm is used to compare two paths
 *
  * Given two ambiguous URI patterns A and B with segments [A0, …, An] and [B0, …, Bm] with query string literals [AQ0, …, AQp] and [BQ0, …, BQq]
  * (with both p and q possibly zero, i.e., without query string literals), the following steps are taken to compare them,
  * for each index x from 0 to min(n, m)
  *
  * If A[x] and B[x] are both literals then continue (the literal values have to be equal otherwise the patterns are not ambiguous)
  * If A[x] is a literal and B[x] is a label then A is more specific than B,
  * If A[x] is a non-greedy label and B[x] is a greedy label then A is more specific than B
  * If n > m then A is more specific than B
  * If p > q then A is more specific than B
  */
object HttpEndpointSpec extends SimpleIOSuite with Checkers {

  def httpEndpointDummy(
      path: List[PathSegment],
      staticQueryParams: Map[String, Seq[String]] = Map.empty
  ): Either[HttpEndpointError, HttpEndpoint[_]] =
    HttpEndpoint.cast(
      Schema
        .operation(ShapeId("smithy4s.dummy", "a"))
        .withHints(
          Http(
            NonEmptyString("GET"), {
              val base = path
                .map {
                  case PathSegment.StaticSegment(value) => value
                  case PathSegment.LabelSegment(value)  => s"{$value}"
                  case PathSegment.GreedySegment(value) => value
                }
                .mkString("/")

              val uri =
                if (staticQueryParams.isEmpty) base
                else {
                  val qp = staticQueryParams
                    .map { case (k, v) =>
                      s"$k=${v.mkString(",")}"
                    }
                    .mkString("&")

                  s"$base?$qp"
                }

              NonEmptyString(uri)
            }
          )
        )
        .withInput(Schema.string)
    )

  test("static > label > greedy") {
    val a = httpEndpointDummy(
      List(
        PathSegment.static("abc"),
        PathSegment.static("bcd"),
        PathSegment.label("xyz")
      )
    )
    val b = httpEndpointDummy(path =
      List(
        PathSegment.static("abc"),
        PathSegment.label("xyz"),
        PathSegment.static("cde")
      )
    )
    val c = httpEndpointDummy(path =
      List(
        PathSegment.greedy("xyz"),
        PathSegment.static("bcd"),
        PathSegment.static("cde")
      )
    )
    val d = httpEndpointDummy(path =
      List(
        PathSegment.greedy("xyz"),
        PathSegment.label("bcd"),
        PathSegment.static("cde")
      )
    )

    List(a, b, c, d).sequence
      .map { (expectedOrder: List[HttpEndpoint[_]]) =>
        forall(Gen.long) { seed =>
          val shuffleOrder = new Random(seed).shuffle(expectedOrder)

          expect(
            shuffleOrder.sortWith(HttpEndpoint.moreSpecific) == expectedOrder
          )
        }
      }
      .getOrElse(IO(failure("could not initialize dummy segments")))

  }

  pureTest("A[x] and B[x] are both literals then continue") {
    val a = httpEndpointDummy(path = List(PathSegment.static("abc")))
    val b = httpEndpointDummy(path = List(PathSegment.static("bcd")))

    expectFirstIsMoreSpecificThanSecond(a, b)
  }

  pureTest(
    "A[x] is a literal and B[x] is a label then A is more specific than B"
  ) {
    val a = httpEndpointDummy(path =
      List(PathSegment.static("abc"), PathSegment.static("abc"))
    )
    val b = httpEndpointDummy(path =
      List(PathSegment.static("bcd"), PathSegment.label("xyz"))
    )

    expectFirstIsMoreSpecificThanSecond(a, b)
  }

  pureTest(
    "A[x] is a non-greedy label and B[x] is a greedy label then A is more specific than B"
  ) {
    val a = httpEndpointDummy(path =
      List(PathSegment.static("abc"), PathSegment.label("abc"))
    )
    val b = httpEndpointDummy(path =
      List(PathSegment.static("bcd"), PathSegment.greedy("xyz"))
    )

    expectFirstIsMoreSpecificThanSecond(a, b)
  }

  pureTest("n > m then A is more specific than B") {
    val a = httpEndpointDummy(path =
      List(PathSegment.static("abc"), PathSegment.static("bcd"))
    )
    val b = httpEndpointDummy(path = List(PathSegment.static("efg")))

    expectFirstIsMoreSpecificThanSecond(a, b)
  }

  pureTest("p > q then A is more specific than B") {
    val a = httpEndpointDummy(
      path = List(PathSegment.static("abc")),
      staticQueryParams = Map("a" -> Seq.empty, "b" -> Seq.empty)
    )
    val b = httpEndpointDummy(
      path = List(PathSegment.static("abc")),
      staticQueryParams = Map("a" -> Seq.empty)
    )

    expectFirstIsMoreSpecificThanSecond(a, b)
  }

  private def expectFirstIsMoreSpecificThanSecond(
      a: Either[HttpEndpointError, HttpEndpoint[_]],
      b: Either[HttpEndpointError, HttpEndpoint[_]]
  ): Expectations =
    (a, b)
      .mapN(HttpEndpoint.moreSpecific)
      .map(expect(_))
      .getOrElse(failure("could not initialize dummy segments"))

}
