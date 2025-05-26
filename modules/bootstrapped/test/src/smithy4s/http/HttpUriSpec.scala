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

import munit._
import java.net.URI

final class HttpUriSpec extends FunSuite {

  test("Roundtrip from java.net.URI and back ") {
    val uri = URI.create("http://example.com/foo?bar=2")
    val httpUri = HttpUri.fromURI(uri)
    assertEquals(uri, httpUri.toURI)
  }

  test("Roundtrip from HttpUri and back") {
    val httpUri = HttpUri(
      HttpUriScheme.Http,
      "example.com",
      None,
      IndexedSeq("foo"),
      Map("bar" -> List("2")),
      Option.empty
    )
    val uri = httpUri.toURI
    assertEquals(httpUri, HttpUri.fromURI(uri))
  }
}
