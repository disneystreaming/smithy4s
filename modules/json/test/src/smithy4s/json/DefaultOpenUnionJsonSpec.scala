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

package smithy4s.json

import smithy4s.Blob
import smithy4s.codecs.PayloadError
import smithy4s.example.Foo
import smithy4s.example.SampleOpenUnion
import smithy4s.expect
import smithy4s.Schema

class DefaultOpenUnionJsonSpec extends OpenUnionJsonSpec {

  override def read[A: Schema](blob: Blob): Either[PayloadError, A] =
    Json.read(blob)
  override def write[A: Schema](a: A): Blob =
    Json.writeBlob(a)

  test("open tagged union - fail to decode multiple unknown keys") {
    matches(read[SampleOpenUnion](Blob("""{"foo": "bar", "baz": "qux"}"""))) {
      case Left(ex) =>
        expect(
          ex.getMessage.contains("""Expected no other field after 'foo'""")
        )
    }
  }

  test("tagged union - decoding still fails if invalid tag is present") {
    matches(read[Foo](Blob("""{"foo": "bar"}"""))) { case Left(ex) =>
      expect(
        ex.getMessage.contains("""illegal value of discriminator field "foo"""")
      )
    }
  }

}
