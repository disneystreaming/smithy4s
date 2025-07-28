---
sidebar_label: Default Values
title: Default Values
---

## Null Default

In general, the `smithy.api#default` trait with a `null` value has no effect. The only exception is when the shape is explicitly [annotated as nullable](./01-customisation/13-nullable-values.md), in which case Smithy4s will then treat `Nullable.Null` as the default value.

```smithy
structure Test {
  @default // same thing as @default(null)
  one: String
}
```

## Decoding and defaults

The following table shows different scenarios for decoding of a structure named `Foo` with a single field `s`. The type of the field will differ depending on the scenario. However, the input for each scenario is the same: an empty JSON Object (`{}`). We are using JSON to show this behavior (based on the smithy4s json module), but the same is true of how smithy4s decodes `Document` with `Document.DObject(Map.empty)` as an input.

| Required | Nullable | Null Default | Scala Representation               | Input: {}                    |
|----------|----------|--------------|------------------------------------|------------------------------|
| false    | true     | true         | `Foo(s: Nullable[String])`         | Foo(Null)                    |
| false    | true     | false        | `Foo(s: Option[Nullable[String]])` | Foo(None)                    |
| false    | false    | true         | `Foo(s: Option[String])`           | Foo(None)                    |
| false    | false    | false        | `Foo(s: Option[String])`           | Foo(None)                    |
| true     | false    | false        | `Foo(s: String)`                   | Missing required field error |
| true     | false    | true         | `Foo(s: String)`                   | Missing required field error |
| true     | true     | false        | `Foo(s: Nullable[String])`         | Missing required field error |
| true     | true     | true         | `Foo(s: Nullable[String])`         | Foo(Null)                    |

#### Key for Table Above

* Required - True if the field is required, false if not (using `smithy.api#required` trait)
* Nullable - True if the field is nullable, false if not (using `alloy#nullable` trait)
* Null Default - True if the field has a default value of null, false if it has no default (using `smithy.api#default` trait)
* Scala Representation - Shows what type is generated for this schema by smithy4s
* Input: {} - Shows the result of what smithy4s will return when decoding the input of an empty JSON object (`{}`)
