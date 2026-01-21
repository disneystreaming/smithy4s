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

package smithy4s.http

import munit._
import java.net.URI
import smithy4s.Platform

final class HttpUriSpec extends FunSuite {

  test("Roundtrip from java.net.URI and back ") {
    val uri = URI.create("http://example.com/foo?bar=2")
    val httpUri = HttpUri.fromURI(uri)
    assertEquals(uri, httpUri.toURI)
  }

  test("Roundtrip from HttpUri and back") {
    val httpUri = HttpUri(
      origin = Some(
        HttpUriOrigin.absolute(
          HttpUriScheme.Http,
          "example.com"
        )
      ),
      path = IndexedSeq("foo"),
      queryParams = HttpUri.queryParamsFromStringMap(
        Map(
          "bar" -> List("2"),
          "baz" -> List("a==2"),
          "qux" -> List("a&b&c")
        )
      ),
      pathParams = Option.empty
    )

    val uri = httpUri.toURI

    assertEquals(
      uri.toString,
      "http://example.com/foo?bar=2&baz=a%3D%3D2&qux=a%26b%26c"
    )
    assertEquals(httpUri, HttpUri.fromURI(uri))
  }

  test("Roundtrip works correctly when HttpUriOrigin is None") {
    val httpUri = HttpUri(
      origin = None,
      path = IndexedSeq("foo"),
      queryParams = HttpUri.queryParamsFromStringMap(
        Map(
          "bar" -> List("2"),
          "baz" -> List("a==2"),
          "qux" -> List("a&b&c")
        )
      ),
      pathParams = Option.empty
    )

    val uri = httpUri.toURI

    assertEquals(
      uri.toString,
      "/foo?bar=2&baz=a%3D%3D2&qux=a%26b%26c"
    )
    assertEquals(httpUri, HttpUri.fromURI(uri))
  }

  test("Roundtrip preserves uri encoding from java.net.URI") {
    if (Platform.isNative)
      assume(
        false,
        "This test is not applicable for Scala Native, as we are on 4.x which has bugs in URI parsing and encoding"
      )
    else {
      // This URI contains spaces, which should be encoded as %20
      val uri = URI.create("http://example.com/foo%20bar?baz=qux%20quux")
      val httpUri = HttpUri.fromURI(uri)
      assertEquals(uri, httpUri.toURI)
      assert(httpUri.path == IndexedSeq("foo bar"))
      assert(httpUri.queryParamsAsMap == Map("baz" -> List(Some("qux quux"))))
    }
  }

  test("Roundtrip preserves uri encoding from HttpUri") {
    if (Platform.isNative)
      assume(
        false,
        "This test is not applicable for Scala Native, as we are on 0.4.x which has bugs in URI parsing and encoding"
      )
    else {
      // This HttpUri contains spaces, which should be encoded as %20, however HttpUri stores data in its decoded form
      // so we expect the spaces to be present in the path and query parameters.
      val httpUri = HttpUri(
        origin = Some(
          HttpUriOrigin.absolute(
            HttpUriScheme.Http,
            "example.com"
          )
        ),
        path = IndexedSeq("foo bar"),
        queryParams =
          HttpUri.queryParamsFromStringMap(Map("baz" -> List("qux quux"))),
        pathParams = Option.empty
      )
      val uri = httpUri.toURI
      assertEquals(httpUri, HttpUri.fromURI(uri))
      assert(uri.getRawPath == "/foo%20bar")
      assert(uri.getRawQuery == "baz=qux%20quux")
    }
  }

  test("Parse URI with valueless query parameter") {
    val uri = URI.create("http://example.com/foo?bar&baz=value")
    val httpUri = HttpUri.fromURI(uri)
    assertEquals(
      httpUri.queryParams,
      IndexedSeq("bar" -> None, "baz" -> Some("value"))
    )
  }

  test("HttpUri with valueless param converts to URI correctly") {
    val httpUri = HttpUri(
      origin = Some(
        HttpUriOrigin.absolute(
          HttpUriScheme.Http,
          "example.com"
        )
      ),
      path = IndexedSeq("foo"),
      queryParams = IndexedSeq("bar" -> None, "baz" -> Some("value")),
      pathParams = Option.empty
    )
    val uri = httpUri.toURI
    assertEquals(uri.toString, "http://example.com/foo?bar&baz=value")
  }

  test("Roundtrip with valueless query parameters") {
    val uri = URI.create("http://example.com/foo?flag&key=value")
    val httpUri = HttpUri.fromURI(uri)
    assertEquals(uri, httpUri.toURI)
  }

  test("queryParamsAsMap preserves None values") {
    val httpUri = HttpUri(
      origin = None,
      path = IndexedSeq("foo"),
      queryParams = IndexedSeq(
        "a" -> None,
        "b" -> Some(""),
        "c" -> Some("value")
      ),
      pathParams = Option.empty
    )
    assertEquals(
      httpUri.queryParamsAsMap,
      Map("a" -> Seq(None), "b" -> Seq(Some("")), "c" -> Seq(Some("value")))
    )
  }

  test("Distinguish between empty value and valueless params") {
    val uriEmpty = URI.create("http://example.com/foo?param=")
    val uriValueless = URI.create("http://example.com/foo?param")

    val httpUriEmpty = HttpUri.fromURI(uriEmpty)
    val httpUriValueless = HttpUri.fromURI(uriValueless)

    assertEquals(httpUriEmpty.queryParams, IndexedSeq("param" -> Some("")))
    assertEquals(httpUriValueless.queryParams, IndexedSeq("param" -> None))
  }

  test("Multiple valueless query parameters") {
    val uri = URI.create("http://example.com/foo?flag1&flag2&key=value")
    val httpUri = HttpUri.fromURI(uri)
    assertEquals(
      httpUri.queryParams,
      IndexedSeq(
        "flag1" -> None,
        "flag2" -> None,
        "key" -> Some("value")
      )
    )
  }

}
