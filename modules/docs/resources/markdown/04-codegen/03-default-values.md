---
sidebar_label: Default Values
title: Default Values
---

## Null Default

When the `smithy.api#default` trait annotating a shape contains a `null` value, and the shape is not additionally [annotated explicitly as nullable](./01-customisation/13-nullable-values.md), Smithy4s will (where possible) assume a "zero value" as the default. For example:

```smithy
structure Test {
  @default // same thing as @default(null)
  one: String
}
```

Here the default for the field `one` will be assumed to be an empty string (`""`). Below is a table showing what all the zero values are for each different Smithy shape type:

| Smithy Type | Zero Value            |
|-------------|-----------------------|
| blob        | Array.empty           |
| boolean     | false                 |
| string      | ""                    |
| byte        | 0                     |
| short       | 0                     |
| integer     | 0                     |
| long        | 0                     |
| float       | 0                     |
| double      | 0                     |
| bigInteger  | 0                     |
| bigDecimal  | 0                     |
| timestamp   | 0 epoch (01 Jan 1970) |
| document    | Document.DNull        |
| enum        | N/A                   |
| intEnum     | N/A                   |
| list        | List.empty            |
| map         | Map.empty             |
| structure   | N/A                   |
| union       | N/A                   |
| service     | N/A                   |
| operation   | N/A                   |
| resource    | N/A                   |

Not every shape type has a corresponding zero value. For example, there is no reasonable zero value for a structure or a union type. As such, they will not have a zero value set even if they are marked with a null default trait.

## Decoding and defaults

The following table shows different scenarios for decoding of a structure named `Foo` with a single field `s`. The type of the field will differ depending on the scenario. However, the input for each scenario is the same: an empty JSON Object (`{}`). We are using JSON to show this behavior (based on the smithy4s json module), but the same is true of how smithy4s decodes `Document` with `Document.DObject(Map.empty)` as an input.

| Required | Nullable | Null Default | Scala Representation               | Input: {}                    |
|----------|----------|--------------|------------------------------------|------------------------------|
| false    | true     | true         | `Foo(s: Nullable[String])`         | Foo(Null)                    |
| false    | true     | false        | `Foo(s: Option[Nullable[String]])` | Foo(None)                    |
| false    | false    | true         | `Foo(s: String)`                   | Foo("")                      |
| false    | false    | false        | `Foo(s: Option[String])`           | Foo(None)                    |
| true     | false    | false        | `Foo(s: String)`                   | Missing required field error |
| true     | false    | true         | `Foo(s: String)`                   | Foo("")                      |
| true     | true     | false        | `Foo(s: Nullable[String])`         | Missing required field error |
| true     | true     | true         | `Foo(s: Nullable[String])`         | Foo(Null)                    |

#### Key for Table Above

* Required - True if the field is required, false if not (using `smithy.api#required` trait)
* Nullable - True if the field is nullable, false if not (using `alloy#nullable` trait)
* Null Default - True if the field has a default value of null, false if it has no default (using `smithy.api#default` trait)
* Scala Representation - Shows what type is generated for this schema by smithy4s
* Input: {} - Shows the result of what smithy4s will return when decoding the input of an empty JSON object (`{}`)
