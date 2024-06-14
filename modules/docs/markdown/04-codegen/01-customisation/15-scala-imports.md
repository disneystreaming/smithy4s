---
sidebar_label: ScalaImports
title: Add Scala imports to generated code
---

`scalaImports` trait provides a mechanism for adding additional imports to smithy4s's generated code. This can be particularly useful when you want to combine type refinements (especially when the type refinements come a third party or in other module) and validators.

Lets say We have a smithy specification and it's accommodated Scala code as below:

```kotlin
$version: "2.0"
namespace test

use smithy4s.meta#refinement

@trait(selector: "integer")
@refinement(
    targetType: "myapp.types.PositiveInt"
    providerImport: "myapp.types.providers._"
)
structure PageSizeFormat { }

@PageSizeFormat
integer PageSize

structure Input {
  pageSize: PageSize
}

```

And

```scala mdoc:reset:invisible
// this is just here so the lower blocks will compile
import smithy4s._
import smithy4s.schema.Schema._
case class PageSizeFormat()

object PageSizeFormat extends ShapeTag.Companion[PageSizeFormat] {
  val id: ShapeId = ShapeId("smithy4s", "PageSizeFormat")

  val hints: Hints = Hints(
    smithy.api.Trait(selector = Some("integer"), structurallyExclusive = None, conflicts = None, breakingChanges = None),
  ).lazily


  implicit val schema: Schema[PageSizeFormat] = constant(PageSizeFormat()).withId(id).addHints(hints)
}
```

```scala mdoc:silent
// package myapp.types
// The recommendations from Type refinements docs are also applied here
import smithy4s._

case class PositiveInt(value: Int)
object PositiveInt {

  private def isPositiveInt(value: Int): Boolean = value > 0

  def apply(value: Int): Either[String, PositiveInt] =
    if (isPositiveInt(value)) Right(new PositiveInt(value))
    else Left(s"$value is not a positive int")
}

object providers {

  implicit val provider: RefinementProvider[PageSizeFormat, Int, PositiveInt] = Refinement.drivenBy[PageSizeFormat](
    PositiveInt.apply,
    (i: PositiveInt) => i.value
  )
}
```

:::info

Note that the implicit `RefinementProvider` is not in the companion object of `PositiveInt`, so that We need to add an `providerImport` to the `refinement` trait.

:::

What We have here is `PageSize` will be generated as a `PositiveInt`. Which is really nice, but what if you need another validator like `@range` to limit how big a `pageSize` can be? So, just let try:

```kotlin
structure Input {
  // highlight-start
  @range(max: 100)
  // highlight-end
  pageSize: PageSize
}
```

And compile to see what happen:

```
[error] 20 |    PageSize.schema.validated(smithy.api.Range(min = None, max = Some(scala.math.BigDecimal(100.0)))).field[Input]("pageSize", _.pageSize).addHints(smithy.api.Default(smithy4s.Document.fromDouble(0.0d))),
[error]    |                                                                                                     ^
[error]    |No implicit value of type smithy4s.RefinementProvider.Simple[smithy.api.Range, test.PageSize] was found for parameter constraint of method validated in trait Schema.
[error]    |I found:
[error]    |
[error]    |    smithy4s.RefinementProvider.isomorphismConstraint[smithy.api.Range, A,
[error]    |      test.PageSize.Type](smithy4s.RefinementProvider.enumRangeConstraint[A],
[error]    |      /* missing */summon[smithy4s.Bijection[A, test.PageSize.Type]])
[error]    |
[error]    |But no implicit values were found that match type smithy4s.Bijection[A, test.PageSize.Type].
[error] one error found
```

This looks scary, but it basically says that We need to create an implicit value of `RefinementProvider.Simple[Range, PageSize.Type]` and provide it to the file that contains `Input` structure. We can do the first step by adding this to `providers` object:

```scala
   implicit val rangeProvider: RefinementProvider.Simple[smithy.api.Range, PositiveInt] =
     RefinementProvider.rangeConstraint(x => x.value)
```
:::info

Note that the `PageSize.Type` is `PositiveInt`

:::

And for the second step, We need to apply `scalaImports` trait with an appropriate import to `Input` structure:

```kotlin
namespace test
// highlight-start
use smithy4s.meta#scalaImports
// highlight-end
use smithy4s.meta#refinement

@trait(selector: "integer")
@refinement(
   targetType: "myapp.types.Natural"
   providerImport: "myapp.types.providers._"
)
structure PageSizeFormat {}

@PageSizeFormat
integer PageSize

// highlight-start
@scalaImports(["myapp.types.providers._"])
// highlight-end
structure Input {
  @range(max: 100)
  pageSize: PageSize
}
```

Now, Smithy4s will validate any `PageSize` value against range and then refine it into `PositiveInt`. We have the best of both worlds.
