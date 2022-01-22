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

import cats.effect._
import cats.syntax.all._

/**
 * This spec dynamically compiles a KV Store service,
 * creates a dummy instance for it (that returns 0 values for all the fields of outputs),
 * and serves it over an example JSON-RPC-like protocol (returning a `Document => IO[Document]`)
 */
object DynamicJsonServerSpec extends weaver.IOSuite {

  case class JsonIO(fn: Document => IO[Document]) {
    def apply(json: Document): IO[Document] = fn(json)
  }

  type Res = JsonIO

  val modelString = """|namespace foo
                       |
                       |service KVStore {
                       |  operations: [Get, Set, Delete]
                       |}
                       |
                       |operation Set {
                       |  input: KeyValue
                       |}
                       |
                       |operation Get {
                       |  input: Key,
                       |  output: Value
                       |}
                       |
                       |operation Delete {
                       |  input: Key
                       |}
                       |
                       |structure Key {
                       |  @required
                       |  key: String
                       |}
                       |
                       |structure KeyValue {
                       |  @required
                       |  key: String,
                       |  @required
                       |  value: String
                       |}
                       |
                       |structure Value {
                       |  @required
                       |  value: String
                       |}
                       |""".stripMargin

  def sharedResource: Resource[IO, Res] = Resource.eval {
    Utils
      .compile(modelString)
      .flatMap { dynamicModel =>
        dynamicModel.allServices
          .find(_.service.id == ShapeId("foo", "KVStore"))
          .liftTo[IO](new Throwable("Not found"))
      }
      .map(service => JsonIOProtocol.dummy(service.service))
      .map(JsonIO(_))
  }

  test("Dynamic service is correctly wired: Set") { jsonIO =>
    val expected = Document.obj()

    jsonIO(
      Document.obj(
        "Set" -> Document.obj(
          "key" -> Document.fromString("K"),
          "value" -> Document.fromString("V")
        )
      )
    ).map { result =>
      expect.same(result, expected)
    }
  }

  test("Dynamic service is correctly wired: Get") { jsonIO =>
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

  test("Dynamic service is correctly wired: Bad Json Input") { jsonIO =>
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

  test("Dynamic service is correctly wired: Bad operation") { jsonIO =>
    val expected = JsonIOProtocol.NotFound

    jsonIO(
      Document.obj("Unknown" -> Document.DNull)
    ).attempt.map { result =>
      expect(result == Left(expected))
    }
  }

}
