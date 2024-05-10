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
