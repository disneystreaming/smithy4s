/*
 *  Copyright 2021-2023 Disney Streaming
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

package smithy4s.aws

import smithy4s.Blob
import cats.syntax.all._
import smithy4s.interopcats._
import smithy4s.http._
import cats.effect.IO
import weaver._

// scalafmt: { maxColumn = 120 }
object AwsJsonErrorTypeDecoderTest extends SimpleIOSuite {

  val discriminators =
    List(
      "FooError",
      "FooError:http://internal.amazon.com/coral/com.amazon.coral.validate/",
      "aws.protocoltests.restjson#FooError",
      "aws.protocoltests.restjson#FooError:http://internal.amazon.com/coral/com.amazon.coral.validate/"
    )

  val fromJsonResponse =
    AwsErrorTypeDecoder.fromResponse[IO](
      smithy4s.aws.internals.AwsJsonCodecs.jsonDecoders
    )

  val responseBase = HttpResponse(404, Map.empty, Blob.empty)

  test("Finds discriminator from header") {
    discriminators.foldMap { disc =>
      val httpResponse =
        responseBase.addHeader("X-Amzn-Errortype", disc)
      fromJsonResponse(httpResponse).map { result =>
        expect.same(result, HttpDiscriminator.NameOnly("FooError"))
      }
    }
  }

  test("Finds discriminator from code field") {
    discriminators.foldMap { disc =>
      val httpResponse = responseBase.withBody(Blob(s"""{"code":"$disc"}"""))
      fromJsonResponse(httpResponse).map { result =>
        expect.same(result, HttpDiscriminator.NameOnly("FooError"))
      }
    }
  }

  test("Finds discriminator from type field") {
    discriminators.foldMap { disc =>
      val httpResponse = responseBase.withBody(Blob(s"""{"__type":"$disc"}"""))
      fromJsonResponse(httpResponse).map { result =>
        expect.same(result, HttpDiscriminator.NameOnly("FooError"))
      }
    }
  }

  test("Surface absence of discriminator") {
    discriminators.foldMap { disc =>
      val httpResponse = responseBase.withBody(Blob("{}"))
      fromJsonResponse(httpResponse).map { result =>
        expect.same(result, HttpDiscriminator.Undetermined)
      }
    }
  }

}
