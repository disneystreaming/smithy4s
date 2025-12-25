---
sidebar_label: Default rendering
title: Default rendering
---

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
