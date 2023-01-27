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

package smithy4s
package dynamic
import cats.syntax.all._
import smithy4s.example.KVStore

import DummyIO._

/**
 * This spec dynamically compiles a KV Store service,
 * creates a dummy instance for it (that returns 0 values for all the fields of outputs),
 * and serves it over an example JSON-RPC-like protocol (returning a `Document => IO[Document]`)
 */
class DynamicJsonProxySpec() extends munit.FunSuite {

  def kvStoreResource: IO[KVStore[IO]] = {
    IO(scala.collection.mutable.Map.empty[String, String])
      .map(new KVStoreImpl(_))
      .flatMap { kvstore =>
        val jsonIOService = JsonIOProtocol.toJsonF(kvstore)
        Utils
          .compileSampleSpec("kvstore.smithy")
          .flatMap { dynamicSchemaIndex =>
            dynamicSchemaIndex
              .getService(ShapeId("smithy4s.example", "KVStore"))
              .liftTo[IO](new Throwable("Not found"))
          }
          .map { serviceDef =>
            JsonIOProtocol.redactingProxy(jsonIOService, serviceDef.service)
          }
          .map { proxied =>
            JsonIOProtocol.fromJsonF(proxied)(KVStore.service)
          }
      }
  }

  test("Static service is correctly proxied by dynamic service") {
    kvStoreResource
      .flatMap { kvStore =>
        for {
          _ <- kvStore.put("key", "value")
          v <- kvStore.get("key")
        } yield expect(v.value === "value")
      }
      .check()
  }

  test("Dynamic service based proxy propagates errors correctly") {
    // This verifies that errors are actually exercised by the dynamic proxy,
    // which redacts strings that are equal to "sensitive". If the dynamic proxy
    // didn't have knowledge of errors, it would fail to decode the document
    // that represents the capture error, and would have raised a `PayloadError` instead.
    //
    // Because we get the redacted string, it means that the dynamic proxy was able to decode
    // the error, and re-encode it, hiding the sensitive string.
    kvStoreResource
      .flatMap { kvStore =>
        val expectedError = smithy4s.example.KeyNotFoundError("*****")

        for {
          attempt <- kvStore.get("sensitive").attempt
        } yield expect.same(attempt, Left(expectedError))
      }
      .check()

  }

  test("Dynamic service based proxy supports service errors") {
    kvStoreResource
      .flatMap { kvStore =>
        val expectedError =
          smithy4s.example.UnauthorizedError("*****")

        for {
          attempt <- kvStore.get("authorized-only-key").attempt
        } yield {
          expect.same(attempt, Left(expectedError))
        }
      }
      .check()
  }

}
