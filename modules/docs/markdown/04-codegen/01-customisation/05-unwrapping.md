---
sidebar_label: Unwrapping
title: New types (and unwrapping)
---

By default, smithy4s will wrap all standalone primitive types in a Newtype. A standalone primitive type is one that is defined like the following:

```smithy
string Email // standalone primitive

structure Test {
  email: Email
  other: String // not a standalone primitive
}
```

Given this example, smithy4s would generate something like the following:

```scala
final case class Test(email: Email, other: String)
```

This wrapping may be undesirable in some circumstances. As such, we've provided the `smithy4s.meta#unwrap` trait. This trait tells the smithy4s code generation to not wrap these types in a newtype when they are used.

```smithy
use smithy4s.meta#unwrap

@unwrap
string Email

structure Test {
  email: Email
  other: String
}
```

This would now generate something like:

```scala
final case class Test(email: String, other: String)
```

This can be particularly useful when working with refinement types (see above for details on refinements). By default, any type that is `refined` will be generated inside of a newtype. If you don't want this, you can mark the type with the `unwrap` trait.

```smithy
@trait(selector: "string")
structure emailFormat {}

@emailFormat
@unwrap
string Email
```

:::info

By default, smithy4s renders collection types as unwrapped EXCEPT when the collection has been refined. In this case, the collection will be rendered within a newtype by default. If you wish your refined collection be rendered unwrapped, you can accomplish this using the same `@unwrap` trait annotation on it.

:::
