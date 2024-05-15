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

  test("Handler composition: orElse ") {

    val ref = new AtomicReference(Map.empty[String, String])
    val kvStore: KVStore[Id] = get(ref)
      .orElse(put(ref))
      .orElse(delete(ref))
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
      .fromHandlers(
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
