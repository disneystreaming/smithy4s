---
sidebar_label: Wildcard types
title: Scala wildcard type arguments
---

Smithy4s has the ability to render optics (Lens/Prism) instances in the code it generates.

If you're using Smithy4s via `mill` or `sbt`, then you can enable this functionality with the following keys:

* in mill, task: `def smithy4sRenderOptics = true`
* in sbt, setting: `smithy4sRenderOptics := true`

If you are using Smithy4s via the CLI, then they way to utilize this feature is through your Smithy specifications. The simplest approach is to add a file with the following content to your CLI invocation:

```kotlin
$version: "2"

metadata smithy4sRenderOptics = true
```

## Optics Usage

Below is an example of using the lenses that smithy4s generates. By default, smithy4s will generate lenses for all structure shapes in your input smithy model(s).

```scala mdoc:reset
import smithy4s.example._

val input = EchoInput("test", EchoBody(Some("test body")))
val lens = EchoInput.Optics.body.andThen(EchoBody.Optics.data).some
val resultGet = lens.getOption(input)

resultGet == Option("test body") // true

val resultSet =
  lens.replace("new body")(input)

val updatedInput = EchoInput("test", EchoBody(Some("new body")))

resultSet == updatedInput // true
```

You can also compose prisms with lenses (and vice-versa) as in the example below:

```scala mdoc:reset
import smithy4s.example._

val input = Podcast.Video(Some("Pod Title"))

// Podcast.Optics.video is a Lens and Podcast.Video.Optics.title is a Prism
val prism = Podcast.Optics.video.andThen(Podcast.Video.Optics.title).some
val result = prism.replace("New Pod Title")(input)

Podcast.Video(Some("New Pod Title")) == result // true
```

Smithy4s also provides a `value` function on Prisms and Lenses that can be used to abstract over NewTypes (similar to what `.some` does for Option types):

```scala mdoc:reset
import smithy4s.example._

val input = GetCityInput(CityId("test"))

val cityName: smithy4s.optics.Lens[GetCityInput, String] = GetCityInput.Optics.cityId.value
val updated = cityName.replace("Fancy New Name")(input)

val result = cityName.getOption(updated)

Option("Fancy New Name") == result // true
```

## Using 3rd Party Optics Libraries

If you'd like to use a third party optics library for more functionality, you can accomplish this by adding an object with a few conversion functions. Here is an example using [Monocle](https://www.optics.dev/Monocle/).

```scala mdoc:reset
object MonocleConversions {

  implicit def smithy4sToMonocleLens[S, A](
      smithy4sLens: smithy4s.optics.Lens[S, A]
  ): monocle.Lens[S, A] =
    monocle.Lens[S, A](smithy4sLens.get)(smithy4sLens.replace)

  implicit def smithy4sToMonoclePrism[S, A](
      smithy4sPrism: smithy4s.optics.Prism[S, A]
  ): monocle.Prism[S, A] =
    monocle.Prism(smithy4sPrism.getOption)(smithy4sPrism.project)

  implicit def smithy4sToMonocleOptional[S, A](
      smithy4sOptional: smithy4s.optics.Optional[S, A]
  ): monocle.Optional[S, A] =
    monocle.Optional(smithy4sOptional.getOption)(smithy4sOptional.replace)

}
```

Then you can `import MonocleConversions._` at the top of any file you need to seamlessly convert smithy4s optics over to Monocle ones.
