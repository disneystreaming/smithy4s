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

package smithy4s.other

import munit.FunSuite
import smithy4s.example.KVStoreOperation
import cats.Id
import java.util.concurrent.atomic.AtomicReference
import smithy4s.example.Value
import smithy4s.example.KVStore
import smithy4s.example.Key

class HandlerSpec extends FunSuite {

  def get(ref: AtomicReference[Map[String, String]]) =
    KVStoreOperation.Get.handler[Id] { input =>
      Value(ref.get().get(input.key).getOrElse("unknown"))
    }

  def put(ref: AtomicReference[Map[String, String]]) =
    KVStoreOperation.Put.handler[Id] { input =>
      val _ = ref.updateAndGet(_ + (input.key -> input.value))
    }

  // Handlers can also be created by inheriting from a class.
  case class delete(ref: AtomicReference[Map[String, String]])
      extends KVStoreOperation.Delete.Handler[Id] {
    def run(input: Key): Id[Unit] = {
      val _ = ref.updateAndGet(_ - input.key)
    }
  }

  test("Handler composition: combine ") {

    val ref = new AtomicReference(Map.empty[String, String])
    val kvStore: KVStore[Id] = get(ref)
      .or(put(ref))
      .or(delete(ref))
      .asService(KVStore)
      .throwing

    kvStore.put("a", "b")
    val b = kvStore.get("a").value
    kvStore.delete("a")

    assertEquals(b, "b")
    assertEquals(ref.get(), Map.empty[String, String])
  }

  test("Handler composition: combineAll ") {
    val ref = new AtomicReference(Map.empty[String, String])
    val kvStore: KVStore[Id] = KVStore
      .fromFunctorHandlers[Id](
        get(ref),
        put(ref),
        delete(ref)
      )
      .throwing

    kvStore.put("a", "b")
    val b = kvStore.get("a").value
    kvStore.delete("a")

    assertEquals(b, "b")
    assertEquals(ref.get(), Map.empty[String, String])
  }

}
