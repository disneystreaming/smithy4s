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

import weaver._
import scala.util.Random

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
object HttpEndpointSpec extends SimpleIOSuite {

  final case class HttpEndpointDummy(
      path: List[PathSegment],
      staticQueryParams: Map[String, Seq[String]] = Map.empty
  ) extends HttpEndpoint[Unit] {
    override def path(input: Unit): List[String] =
      throw new NotImplementedError(
        "HttpEndpointOrderingSpec.HttpEndpointDummy.path"
      )
    override def method: HttpMethod = throw new NotImplementedError(
      "HttpEndpointOrderingSpec.HttpEndpointDummy.method"
    )
    override def code: Int = throw new NotImplementedError(
      "HttpEndpointOrderingSpec.HttpEndpointDummy.code"
    )
  }

  pureTest("static > label > greedy") {
    val a = HttpEndpointDummy(path =
      List(
        PathSegment.static("abc"),
        PathSegment.static("bcd"),
        PathSegment.label("xyz")
      )
    )
    val b = HttpEndpointDummy(path =
      List(
        PathSegment.static("abc"),
        PathSegment.label("xyz"),
        PathSegment.static("cde")
      )
    )
    val c = HttpEndpointDummy(path =
      List(
        PathSegment.greedy("xyz"),
        PathSegment.static("bcd"),
        PathSegment.static("cde")
      )
    )
    val d = HttpEndpointDummy(path =
      List(
        PathSegment.greedy("xyz"),
        PathSegment.label("bcd"),
        PathSegment.static("cde")
      )
    )

    val expectedOrder = List[HttpEndpoint[_]](a, b, c, d)
    val shuffleOrder = Random.shuffle(expectedOrder)

    expect(shuffleOrder.sortWith(HttpEndpoint.moreSpecific) == expectedOrder)
  }

  pureTest("A[x] and B[x] are both literals then continue") {
    val a = HttpEndpointDummy(path = List(PathSegment.static("abc")))
    val b = HttpEndpointDummy(path = List(PathSegment.static("bcd")))

    expect(HttpEndpoint.moreSpecific(a, b))
  }

  pureTest(
    "A[x] is a literal and B[x] is a label then A is more specific than B"
  ) {
    val a = HttpEndpointDummy(path =
      List(PathSegment.static("abc"), PathSegment.static("abc"))
    )
    val b = HttpEndpointDummy(path =
      List(PathSegment.static("bcd"), PathSegment.label("xyz"))
    )

    expect(HttpEndpoint.moreSpecific(a, b))
  }

  pureTest(
    "A[x] is a non-greedy label and B[x] is a greedy label then A is more specific than B"
  ) {
    val a = HttpEndpointDummy(path =
      List(PathSegment.static("abc"), PathSegment.label("abc"))
    )
    val b = HttpEndpointDummy(path =
      List(PathSegment.static("bcd"), PathSegment.greedy("xyz"))
    )

    expect(HttpEndpoint.moreSpecific(a, b))
  }

  pureTest("n > m then A is more specific than B") {
    val a = HttpEndpointDummy(path =
      List(PathSegment.static("abc"), PathSegment.static("bcd"))
    )
    val b = HttpEndpointDummy(path = List(PathSegment.static("efg")))

    expect(HttpEndpoint.moreSpecific(a, b))
  }

  pureTest("p > q then A is more specific than B") {
    val a = HttpEndpointDummy(
      path = List(PathSegment.static("abc")),
      staticQueryParams = Map("a" -> Seq.empty, "b" -> Seq.empty)
    )
    val b = HttpEndpointDummy(
      path = List(PathSegment.static("abc")),
      staticQueryParams = Map("a" -> Seq.empty)
    )

    expect(HttpEndpoint.moreSpecific(a, b))
  }
}
