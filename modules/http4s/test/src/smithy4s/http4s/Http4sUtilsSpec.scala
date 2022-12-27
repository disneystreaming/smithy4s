/*
 *  Copyright 2021-2022 Disney Streaming
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
package http4s

import weaver.FunSuite

object Http4sUtilsSpec extends FunSuite {

  test(
    "Path segments with a query string param should properly partitioned into a tuple of path and query params"
  ) {
    val segments = List("foo", "bar", "baz?qux=quz")
    val (path, queries) = splitPathSegmentsAndQueryParams(segments)
    expect.eql(List("foo", "bar", "baz"), path) && expect.eql(
      Map("qux" -> List("quz")),
      queries
    )
  }
  test(
    "Path segments with a query string param WITHOUT A VALUE should properly partitioned into a tuple of path and query params"
  ) {
    val segments = List("foo", "bar", "baz?qux=")
    val (path, queries) = splitPathSegmentsAndQueryParams(segments)
    expect.eql(List("foo", "bar", "baz"), path) && expect.eql(
      Map("qux" -> List("")),
      queries
    )
  }
  test(
    "Path segments with a query string param WITHOUT AN EQUAL SIGN OR VALUE should properly partitioned into a tuple of path and query params"
  ) {
    val segments = List("foo", "bar", "baz?qux")
    val (path, queries) = splitPathSegmentsAndQueryParams(segments)
    expect.eql(List("foo", "bar", "baz"), path) && expect.eql(
      Map("qux" -> List("")),
      queries
    )
  }
}
