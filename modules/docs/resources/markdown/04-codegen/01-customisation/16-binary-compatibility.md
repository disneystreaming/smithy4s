---
title: Binary compatibility
---

By default, Smithy4s generates code that follows idiomatic Scala practices, such as:

- using case classes to represent product shapes
- using sealed traits (ADTs) to represent union and enum shapes.

This approach allows users to treat generated code like they would their own case classes, including having a `copy` method, pattern matching via `unapply`, and so on.

However, it does not play well with [binary compatibility rules][bincompat-jdk], nor does it follow the [binary compatibility for library authors][bincompat-for-libs] principles.

## The problem with case classes

A change as simple as adding a member to a Smithy structure:

```diff
structure Hello {
  @required name: String
+ age: Integer
}
```

may seem simple and harmless when we look at the generated code:

```diff
final case class Hello(
  name: String,
+ age: Option[Int] = None
)
```

but it's not! In fact, this change modifies the signatures of the `copy`, `apply` and `unapply` methods, effectively removing the old methods and adding new ones.

This can be a problem: imagine `Hello` is part of a library you're publishing. Or it's part of `smithy4s-core` itself.
When the Smithy model for `Hello` is changed like that, the library is no longer backward compatible with its previous versions. So, the library's author may have to acknowledge and commit to the binary compatibility breakage, and publish a new major version.

## The problem with exhaustive pattern matching

A similar problem, although not entirely part of what's considered "binary compatibility": exhaustive pattern matching.

Consider a **library A** was published at `v1` with the following generated enum:

```scala
sealed abstract class WeatherType
object WeatherType {
  case object WINDY extends WeatherType
  case object RAINY extends WeatherType
  //...
}
```

There may be code somewhere, perhaps in a **library B** that sits between **library A** and your application:

```scala
val handle: WeatherType => Unit = {
  case WINDY => println("it's windy!")
  case RAINY => println("it's not")
}
```

Let it be library B's `v20` version. It's built and published against A's `v1`.

If a new value gets added to `WeatherType` (say, `WeatherType.SUNNY`) in library A's `v2`, and you have the following dependencies:

- A: `v2`
- B: `v20` (depends on A `v1`)

Your dependency resolution will most likely _evict_ A's older versions and only use `v2`. Now, if you call `handle(WeatherType.SUNNY)` it'll throw a `scala.MatchError` at runtime.

Only when library B updates its version of A to `v2` or above, will it get a compile-time check: the compiler will complain about a non-exhaustive pattern match, which will need to be fixed before publishing.

## The solution: bincompat-friendly mode

In order to help avoid such issues in code relying on Smithy4s-generated datatypes, we provide a codegen customization trait: `smithy4s.meta#bincompatFriendly` - available starting from smithy4s `0.18.40`.

If you expect your code to be published in a library, you should apply the trait to all shapes used for codegen, before you plan any problematic changes:

```diff
+ use smithy4s.meta#bincompatFriendly

+ @bincompatFriendly
structure Hello {
  @required name: String
}
```

This will change the structure of generated code so that it's possible to evolve in a bincompat safe way.

## Support

At the time of writing, the following shapes can be made bincompat-friendly:

- structures
- unions
- enums and intEnums

With the following exceptions:

- [Error][error-trait] shapes
- Structure shapes that are used directly as an [operation's input/output property](https://smithy.io/2.0/spec/service-types.html#operation)
- [ADT](./02-adts.md) unions and their targets

For the current list of limitations on what can and can't be made bincompat-friendly, see [the tests of `BincompatTraitValidationSpec`][bincompat-trait-tests].

## Adding members to bincompatFriendly shapes

In the case of unions and enums/intEnums, you can safely add members **at any time** without additional hassle.

However, in the case of structures it's more complicated: any new members must be marked with the `smithy4s.meta#bincompatAdded` trait.

For example, with our `Hello` example:

```diff
use smithy4s.meta#bincompatFriendly
+ use smithy4s.meta#bincompatAdded

@bincompatFriendly
structure Hello {
  @required name: String
+ @bincompatAdded(version: "1.0.0")
  age: Integer
}
```

You can add multiple fields with the same version. Each such group (i.e. same version number) will generate an additional `apply` method in the case class's companion object. For example, the following members:

- (unversioned) s1, s2
- (v1.0.0) s3, s4
- (v2.0.0) s5

would generate three `apply` methods, respectively taking the arguments:

- s1, s2
- s1, s2, s3, s4
- s1, s2, s3, s4, s5.

In other words, **adding a new version to the mix** is a bincompat-safe change, but adding a new member to an existing version is still breaking.

Notably, the "baseline" apply method (the one generated for unversioned members only) will have an identical shape to a "normal" generated case class's `apply` method: it can have default values for its parameters, for example.

Additionally, the following limitations apply:

- The version number can be any number of digit groups, separated by dots (`1.1`, `1.2.3`, `0.5.6.7` are all valid)
- Added fields must either be optional (i.e. not have the `required` trait) or have a [default value][default-values].

## Compatibility cheatsheet

Here's a non-exhaustive list of changes that are considered safe or unsafe in bincompat-friendly mode:

| Shape type | Change type                                | Safe?                                                                    |
| ---------- | ------------------------------------------ | ------------------------------------------------------------------------ |
| Struct     | Adding a member                            | :warning: If it's effectively optional and has a valid `@bincompatAdded` |
| Struct     | Adding a default value to a member         | :warning: Only if it was `@required` before                              |
| Any shape  | Changing a member's target                 | :x: No                                                                   |
| Any shape  | Removing/renaming a member                 | :x: No                                                                   |
| Any shape  | Enabling/disabling bincompat-friendly mode | :x: No                                                                   |

:::warning

Please verify the bincompat safety of your changes before you publish them. We recommend that you use the [MiMa](https://github.com/lightbend/mima) plugin for your build tool of choice.

## What does the generated code look like?

There are several changes we make to the codegen process in bincompat-friendly mode, so that the generated datatypes can evolve safely.

### Structures

Although [Binary Compatibility for library authors][bincompat-for-libs] currently encourages continued use of case classes with a private constructor (and a few more tweaks),
Smithy4s has to support all active Scala versions, and each of them has its caveats.

In order to be bincompat-friendly, starting from case classes we'd need to:

- Make the primary constructor private
- Remove `_1`, `_2`, ... methods (on Scala 3)
- Make the `copy` method private
- Remove the primary `apply` method
- Remove the `unapply` method.

Removing these isn't always an option (e.g. you can't really remove the primary `apply` on Scala 2, even with `-Xsource:3` flags).

Instead, we draw inspiration from [Contraband][sbt-contraband] and use a non-case class enhanced with:

- `withXXX` methods (immutable setters), which use an internal private `copy` method
- a "baseline" `apply` method - see [Adding members to bincompatFriendly shapes](#adding-members-to-bincompatfriendly-shapes)
- `equals`, `hashCode`, `toString`.


### Unions

For unions, we need to remove the possibility of exhaustively matching against the known set of members. This is achieved by:

- adding custom `unapply` methods in the members' companion objects

In addition, we need to make sure all implementations of the union's `Visitor` trait have a default case. To this end:

- the `Visitor` trait itself is made `sealed`

This neat trick makes it only possible to subclass `Visitor.Default`, which enforces having a default case.

### Enums

For enums, just like for unions, we need to remove the possibility of exhaustively matching against the known set of members. This is achieved by:

- hiding the `case object`s representing the enum values inside a private object
- replacing them with `val`s of a widened type (the enum).

[bincompat-trait-tests]: https://github.com/disneystreaming/smithy4s/blob/@VERSION@/modules/protocol-tests/test/src/smithy4s/api/validation/BincompatTraitValidationSpec.scala
[error-trait]: https://smithy.io/2.0/spec/type-refinement-traits.html#error-trait
[default-values]: https://smithy.io/2.0/spec/aggregate-types.html#default-values
[bincompat-for-libs]: https://docs.scala-lang.org/overviews/core/binary-compatibility-for-library-authors.html
[bincompat-jdk]: https://docs.oracle.com/javase/specs/jls/se24/html/jls-13.html
[sbt-contraband]: https://www.scala-sbt.org/contraband/
