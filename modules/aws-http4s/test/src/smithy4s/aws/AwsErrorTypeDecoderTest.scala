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
import smithy4s.http.HttpDiscriminator
import smithy4s.http4s.kernel._
import org.http4s._
import cats.effect.IO
import weaver._

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
      ResponseDecoder.rpcSchemaCompiler(
        EntityDecoders.fromCodecAPI[IO](new json.AwsJsonCodecAPI())
      )
    )

  test("Finds discriminator from header") {
    discriminators.foldMap { disc =>
      val httpResponse =
        Response[IO](Status.NotFound).withHeaders("X-Amzn-Errortype" -> disc)
      fromJsonResponse(httpResponse).map { result =>
        expect.same(result, Some(HttpDiscriminator.NameOnly("FooError")))
      }
    }
  }

  test("Finds discriminator from code field") {
    discriminators.foldMap { disc =>
      val httpResponse =
        Response[IO](Status.NotFound)
          .withEntity(s"""{"code":"$disc"}""".getBytes())
      fromJsonResponse(httpResponse).map { result =>
        expect.same(result, Some(HttpDiscriminator.NameOnly("FooError")))
      }
    }
  }

  test("Finds discriminator from type field") {
    discriminators.foldMap { disc =>
      val httpResponse =
        Response[IO](Status.NotFound)
          .withEntity(s"""{"__type":"$disc"}""".getBytes())
      fromJsonResponse(httpResponse).map { result =>
        expect.same(result, Some(HttpDiscriminator.NameOnly("FooError")))
      }
    }
  }

  test("Surface absence of discriminator") {
    discriminators.foldMap { disc =>
      val httpResponse =
        Response[IO](Status.NotFound).withEntity(s"{}".getBytes())
      fromJsonResponse(httpResponse).map { result =>
        expect.same(result, None)
      }
    }
  }

}
