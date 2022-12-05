---
sidebar_label: Unwrapping
title: New types (and unwrapping)
---

By default, smithy4s will wrap all standalone primitive types in a Newtype. A standalone primitive type is one that is defined like the following:

```kotlin
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

```kotlin
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

```kotlin
@trait(selector: "string")
structure emailFormat {}

@emailFormat
@unwrap
string Email
```

:::info

By default, smithy4s renders collection types as unwrapped EXCEPT when the collection has been refined. In this case, the collection will be rendered within a newtype by default. If you wish your refined collection be rendered unwrapped, you can accomplish this using the same `@unwrap` trait annotation on it.

:::

## Default rendering

Smithy4s allows you to customize how defaults on the fields of smithy structures are rendered inside of case classes. There are three options:

- `FULL`
- `OPTION_ONLY`
- `NONE`

The default is `FULL`.

This value is set using metadata which means that the setting will be applied to all the rendering done by smithy4s.

#### FULL

`FULL` means that default values are rendered for all field types. For example:

```kotlin
metadata smithy4sDefaultRenderMode = "FULL"

structure FullExample {
  one: Integer = 1
  two: String
  @required
  three: String
}
```

would render to something like:

```scala
case class FullExample(three: String, one: Int = 1, two: Option[String] = None)
```

Notice how the fields above are ordered. The reason for this is that fields are ordered as:

1. Required Fields
2. Fields with defaults
3. Optional Fields

#### OPTION_ONLY

```kotlin
metadata smithy4sDefaultRenderMode = "OPTION_ONLY"

structure OptionExample {
  one: Integer = 1
  two: String
  @required
  three: String
}
```

would render to something like:

```scala
case class FullExample(one: String, three: String, two: Option[String] = None)
```

Now `one` doesn't have a default rendered and as such it is placed first in the case class.

#### NONE

```kotlin
metadata smithy4sDefaultRenderMode = "NONE"

structure OptionExample {
  one: Integer = 1
  two: String
  @required
  three: String
}
```

would render to something like:

```scala
case class FullExample(one: String, two: Option[String], three: String)
```

Now none of the fields are rendered with defaults. As such, the order of the fields is the same as is defined in the smithy structure.

:::caution

The presence of the `smithy4sDefaultRenderMode` metadata does NOT change the way smithy4s codecs behave. As such, defaults will still be used when decoding
fields inside of clients and servers. This feature is purely for changing the generated code for your convenience.

:::
