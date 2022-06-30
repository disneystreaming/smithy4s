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

package smithy4s
package dynamic

import DummyIO._
import cats.syntax.all._

/**
 * This spec dynamically compiles a KV Store service,
 * creates a dummy instance for it (that returns 0 values for all the fields of outputs),
 * and serves it over an example JSON-RPC-like protocol (returning a `Document => IO[Document]`)
 */
object DynamicJsonServerSpec extends munit.FunSuite {

  case class JsonIO(fn: Document => IO[Document]) {
    def apply(json: Document): IO[Document] = fn(json)
  }

  val sharedResource: IO[JsonIO] = {
    Utils
      .compileSampleSpec("kvstore.smithy")
      .flatMap { DynamicSchemaIndex =>
        DynamicSchemaIndex.allServices
          .find(_.service.id == ShapeId("smithy4s.example", "KVStore"))
          .liftTo[IO](new Throwable("Not found"))
      }
      .map(service => JsonIOProtocol.dummy(service.service))
      .map(JsonIO(_))
  }

  def testJsonIO(name: String)(run: JsonIO => IO[Unit]): Unit =
    test(name) {
      sharedResource.flatMap(run).check
    }

  testJsonIO("Dynamic service is correctly wired: Put") { jsonIO =>
    val expected = Document.obj()

    jsonIO(
      Document.obj(
        "Put" -> Document.obj(
          "key" -> Document.fromString("K"),
          "value" -> Document.fromString("V")
        )
      )
    ).map { result =>
      expect.same(result, expected)
    }
  }

  testJsonIO("Dynamic service is correctly wired: Get") { jsonIO =>
    val expected = Document.obj("value" -> Document.fromString(""))

    jsonIO(
      Document.obj(
        "Get" -> Document.obj(
          "key" -> Document.fromString("K")
        )
      )
    ).map { result =>
      expect.same(result, expected)
    }
  }

  testJsonIO("Dynamic service is correctly wired: Bad Json Input") { jsonIO =>
    val expected = smithy4s.http.PayloadError(
      PayloadPath("key"),
      "",
      "Required field not found"
    )

    jsonIO(
      Document.obj("Get" -> Document.obj())
    ).attempt.map { result =>
      expect(result == Left(expected))
    }
  }

  testJsonIO("Dynamic service is correctly wired: Bad operation") { jsonIO =>
    val expected = JsonIOProtocol.NotFound

    jsonIO(
      Document.obj("Unknown" -> Document.DNull)
    ).attempt.map { result =>
      expect(result == Left(expected))
    }
  }

}
