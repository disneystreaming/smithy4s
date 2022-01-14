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

package smithy4s.http.internals

import smithy4s.Timestamp
import smithy4s.example.DummyServiceGen.DummyPath
import smithy4s.example.PathParams
import smithy4s.http.HttpEndpoint
import smithy4s.http.PathSegment

object PathSpec extends weaver.FunSuite {

  test("Parse path pattern into path segments") {
    val result = pathSegments("/{head}/foo/{tail+}")
    expect(
      result == Option(
        Vector(
          PathSegment.label("head"),
          PathSegment.static("foo"),
          PathSegment.greedy("tail")
        )
      )
    )
  }

  test("Write PathParams") {
    val result = HttpEndpoint
      .cast(
        DummyPath
      )
      .get
      .path(
        PathParams(
          "example with spaces, %, / and \\",
          10,
          Timestamp.fromEpochSecond(0L),
          Timestamp.fromEpochSecond(0L),
          Timestamp.fromEpochSecond(0L),
          Timestamp.fromEpochSecond(0L),
          true
        )
      )

    val expected = if (weaver.Platform.isJS) {
      "dummy-path/example+with+spaces%2C+%25%2C+%2F+and+%5C/10/1970-01-01T00%3A00%3A00.000Z/1970-01-01T00%3A00%3A00.000Z/0/Thu%2C+01+Jan+1970+00%3A00%3A00+GMT/true"
    } else {
      "dummy-path/example+with+spaces%2C+%25%2C+%2F+and+%5C/10/1970-01-01T00%3A00%3A00Z/1970-01-01T00%3A00%3A00Z/0/Thu%2C+01+Jan+1970+00%3A00%3A00+GMT/true"
    }

    assert.eql(result, expected)
  }

}
