---
sidebar_label: Collections
title: Specialised collection types
---

Smithy supports `list` and `set`, Smithy4s renders that to `List[A]` and `Set[A]` respectively. You can also use the `@uniqueItems` annotation on `list` which is equivalent to `set`.

Smithy4s has support for two specialized collection types: `Vector` and `IndexedSeq`. The following examples show how to use them:

```smithy
use smithy4s.meta#indexedSeq
use smithy4s.meta#vector

@indexedSeq
list SomeIndexSeq {
  member: String
}

@vector
list SomeVector {
  member: String
}
```

Both annotations are only applicable on `list` shapes. You can't mix `@vector` with `@indexedSeq`, and neither one can be used with `@uniqueItems`.
