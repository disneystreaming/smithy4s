---
sidebar_label: Nullable Values 
title: Nullable Values
---

By default, smithy does not distinguish between a value being absent and it being null - both are translated to `scala.None`, which in turn will be serialised as an absent value.

In order to differentiate the two, for example in order to allow a HTTP server to implement merge patch semantics or return explicit null rather than silently dropping a field, you can use the `alloy.nullable` trait on members of a structure shape. For example:

```smithy
namespace example

use alloy#nullable

structure Foo {
    @nullable
    a: Integer
}
```


This will be rendered as

```scala
package example

import smithy4s._
import smithy4s.schema.Schema._

final case class Foo(a: Option[Nullable[Int]] = None)

object Foo extends ShapeTag.Companion[Foo] {
  val id: ShapeId = ShapeId("example", "FooIsh")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[Foo] = struct(
    int.nullable.optional[Foo]("a", _.a),
  ){
    Foo.apply
  }.withId(id).addHints(hints)
}
```

The type `Nullable[A]` is an ADT with two members: `Null` and `Value(a)` for some `a`. This makes it exactly equivalent to `Option[A]`, and methods `Nullable.fromOption(option)` and `nullable.toOption` exist to allow for easy conversion between the two types. `Nullable` is used rather than `Option` for having clear semantics.

In this example, `Foo(Some(Nullable.Null))` corresponds to an explicit value of `null` while `Foo(None)` corresponds to absence of a value. This applies to both serialization and deserialization.

## Combinations with other annotations

The annotation `@nullable` can be combined with both `@required` and `@default`, with the following effects:

* annotating as `@required` will forbid the field from being omitted but permit null to be passed explicitly on deserialization. It will always include the field but potentially set it to null on serialization.
* annotating as `@default` works the same as default values for non-nullable fields, with the exception that the default can be set to null and [not automatically adjusted into a "zero value"](../03-default-values.md)

In both cases, the resulting Scala type of the field will be `smithy.Nullable[T]`.
