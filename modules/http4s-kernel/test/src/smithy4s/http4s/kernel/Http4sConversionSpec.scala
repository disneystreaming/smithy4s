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

package smithy4s.http4s.kernel

import weaver._
import org.http4s.implicits._
import smithy4s.http4s.kernel.toSmithy4sHttpUri
import smithy4s.http4s.kernel.fromSmithy4sHttpUri
import org.http4s.Uri

object Http4sConversionSpec extends SimpleIOSuite {
  private def http4sToSmithyAndBackUriTest(input: Uri, output: Uri) = {
    pureTest(s"URI: http4s to smithy4s and back: $input -> $output") {
      assert.eql(
        uri"http://localhost/",
        fromSmithy4sHttpUri(toSmithy4sHttpUri(uri"http://localhost/"))
      )
    }
  }

  http4sToSmithyAndBackUriTest(
    uri"/",
    uri"http://localhost/"
  )

  http4sToSmithyAndBackUriTest(
    uri"/hello",
    uri"http://localhost/hello"
  )

  http4sToSmithyAndBackUriTest(
    uri"http://example.com",
    uri"http://example.com/"
  )

  http4sToSmithyAndBackUriTest(
    uri"example.com",
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

}
