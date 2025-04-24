---
sidebar_label: Decoupled handlers
title: Decoupled handlers
---

Although Smithy4s generates interfaces from smithy services, it is often desirable to split the implementation of individual methods into smaller composable components.

As of 0.18.19, Smithy4s provides functions to facilitate this usecase.

For instance, provided the following specification :

```scala mdoc:passthrough
docs.InlineSmithyFile.fromSample("kvstore.smithy")
```

it is possible to write the following code :

```scala mdoc:silent
import smithy4s.example.KVStoreOperation
import smithy4s.example._
import cats.effect._

def get(ref: Ref[IO, Map[String, String]]) = KVStoreOperation.Get.handler[IO] { input =>
  ref.get.map(_.get(input.key)).flatMap {
    case Some(value) => IO.pure(Value(value))
    case None => IO.raiseError(KeyNotFoundError(s"Key not found: ${input.key}"))
  }
}

def put(ref: Ref[IO, Map[String, String]]) = KVStoreOperation.Put.handler[IO] { input =>
  ref.update(_ + (input.key -> input.value))
}

// Handlers can also be created by inheriting from a class.
class delete(ref: Ref[IO, Map[String, String]]) extends KVStoreOperation.Delete.Handler[IO] {
  def run(input: Key): IO[Unit] = ref.update(_ - input.key)
}
```

And compose the handlers as such :

```scala mdoc:silent
IO.ref(Map.empty[String, String]).map { ref =>
  get(ref)
    .or(put(ref))
    .or(new delete(ref))
    .asService(KVStore)
    .throwing : KVStore[IO]
}
```

or as such :

```scala mdoc:silent
IO.ref(Map.empty[String, String]).map { ref =>
  KVStore.fromHandlers(
    get(ref),
    put(ref),
    new delete(ref)
  ).throwing : KVStore[IO]
}
```

The `.throwing` call indicates that calling un-implemented handlers will throw an exception. Other combinators are available to decide
what behaviour should be applied when un-implemented handlers are called upon.
