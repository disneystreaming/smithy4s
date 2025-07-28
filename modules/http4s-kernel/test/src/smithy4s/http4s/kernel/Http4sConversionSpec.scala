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

package smithy4s.http4s.kernel

import weaver._
import org.http4s.implicits._
import org.http4s.Uri
import smithy4s.http.HttpUriScheme

object Http4sConversionSpec extends SimpleIOSuite {

  // note: these actually work (http4s runs them as HTTP GET against localhost:80)
  http4sToSmithyAndBackUriTest(
    uri"/",
    uri"/"
  )

  http4sToSmithyAndBackUriTest(
    uri"/hello",
    uri"/hello"
  )

  http4sToSmithyAndBackUriTest(
    uri"http://example.com",
    uri"http://example.com/"
  )

  http4sToSmithyAndBackUriTest(
    uri"https://example.com",
    uri"https://example.com/"
  )

  http4sToSmithyAndBackUriTest(
    uri"https://example.com/hello",
    uri"https://example.com/hello"
  )

  http4sToSmithyAndBackUriTest(
    uri"https://example.com/hello?s=42",
    uri"https://example.com/hello?s=42"
  )

  http4sToSmithyAndBackUriTest(
    uri"http://localhost/",
    uri"http://localhost/"
  )

  pureTest("URI: http4s to smithy4s defaults to none") {
    expect.same(
      None,
      toSmithy4sHttpUri(uri"/").scheme
    )
  }

  pureTest("URI: http4s to smithy4s keeps http scheme") {
    expect.same(
      Some(smithy4s.http.HttpUriScheme.Http),
      toSmithy4sHttpUri(uri"http://localhost").scheme
    )
  }

  pureTest("URI: http4s to smithy4s keeps https scheme") {
    expect.same(
      Some(smithy4s.http.HttpUriScheme.Https),
      toSmithy4sHttpUri(uri"https://localhost").scheme
    )
  }

  pureTest("URI: smithy4s to http4s keeps http scheme") {
    expect.same(
      Some(Uri.Scheme.http),
      fromSmithy4sHttpUri(
        aSmithy4sUri(
          scheme = HttpUriScheme.Http
        )
      ).scheme
    )
  }

  pureTest("URI: smithy4s to http4s keeps http scheme") {
    expect.same(
      Some(Uri.Scheme.https),
      fromSmithy4sHttpUri(
        aSmithy4sUri(
          scheme = HttpUriScheme.Https
        )
      ).scheme
    )
  }

  private def http4sToSmithyAndBackUriTest(input: Uri, output: Uri)(implicit
      loc: SourceLocation
  ) = {
    pureTest(s"URI: http4s to smithy4s and back: $input -> $output") {
      val intermediate = toSmithy4sHttpUri(input)

      expect.eql(
        output,
        fromSmithy4sHttpUri(intermediate)
      ) || failure(
        s"comparison failed. Intermediate value for debugging: $intermediate"
      )
    }
  }

  private def aSmithy4sUri(scheme: HttpUriScheme) =
    smithy4s.http.HttpUri.absolute(
      scheme = scheme,
      host = "example.com",
      port = None,
      path = IndexedSeq.empty,
      queryParams = Map.empty,
      pathParams = None
    )
}
