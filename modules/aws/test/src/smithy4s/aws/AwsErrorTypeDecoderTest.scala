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

package smithy4s.aws

import cats.syntax.all._
import smithy4s.http.CaseInsensitive
import weaver._

object AwsJsonErrorTypeDecoderTest extends FunSuite {

  type Xor[A] = Either[Throwable, A]

  val discriminators =
    List(
      "FooError",
      "FooError:http://internal.amazon.com/coral/com.amazon.coral.validate/",
      "aws.protocoltests.restjson#FooError",
      "aws.protocoltests.restjson#FooError:http://internal.amazon.com/coral/com.amazon.coral.validate/"
    )

  val fromJsonResponse = AwsErrorTypeDecoder.fromResponse[Xor](json.awsJson)

  test("Finds discriminator from header") {
    discriminators.foldMap { disc =>
      val httpResponse =
        HttpResponse(
          400,
          List(CaseInsensitive("X-Amzn-Errortype") -> disc),
          Array.emptyByteArray
        )
      val result = fromJsonResponse(httpResponse)
      expect.same(result, Right(Some("FooError")))
    }
  }

  test("Finds discriminator from code field") {
    discriminators.foldMap { disc =>
      val httpResponse =
        HttpResponse(400, List.empty, s"""{"code":"$disc"}""".getBytes())
      val result = fromJsonResponse(httpResponse)
      expect.same(result, Right(Some("FooError")))
    }
  }

  test("Finds discriminator from type field") {
    discriminators.foldMap { disc =>
      val httpResponse =
        HttpResponse(400, List.empty, s"""{"__type":"$disc"}""".getBytes())
      val result = fromJsonResponse(httpResponse)
      expect.same(result, Right(Some("FooError")))
    }
  }

  test("Surface absence of discriminator") {
    discriminators.foldMap { disc =>
      val httpResponse =
        HttpResponse(400, List.empty, "{}".getBytes())
      val result = fromJsonResponse(httpResponse)
      expect.same(result, Right(None))
    }
  }

}
