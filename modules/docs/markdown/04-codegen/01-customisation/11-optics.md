---
sidebar_label: Optics
title: Optics - Lens and Prism
---

Smithy4s renders unions such that the alternatives it renders can be used as Prisms. Similarly, structures are rendered such that their fields can be used as Lenses.

## Optics Usage

Below is an example of using the lenses that smithy4s generates. By default, smithy4s will generate lenses for all structure shapes in your input smithy model(s).

```scala mdoc:reset
import smithy4s.example._

val input = TestInput("test", TestBody(Some("test body")))
val lens = TestInput.body.andThen(TestBody.data).some
val resultGet = lens.project(input)

resultGet == Option("test body") // true

val resultSet =
  lens.replace("new body")(input)

val updatedInput = TestInput("test", TestBody(Some("new body")))

resultSet == updatedInput // true
```

You can also compose prisms with lenses (and vice-versa) as in the example below:

```scala mdoc:reset
import smithy4s.example._

val input = Podcast.Video(Some("Pod Title"))

val prism = Podcast.video.andThen(Podcast.Video.title).some
val result = prism.replace("New Pod Title")(input)

Podcast.Video(Some("New Pod Title")) == result // true
```

Smithy4s also provides a `value` function on Prisms and Lenses that can be used to abstract over NewTypes (similar to what `.some` does for Option types):

```scala mdoc:reset
import smithy4s.example._

val input = GetCityInput(CityId("test"))

val cityName: smithy4s.optics.Lens[GetCityInput, String] = GetCityInput.cityId.value
val updated = cityName.replace("Fancy New Name")(input)

val result = cityName.project(updated)

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
    monocle.Prism(smithy4sPrism.project)(smithy4sPrism.inject)

  implicit def smithy4sToMonocleOptional[S, A](
      smithy4sOptional: smithy4s.optics.Optional[S, A]
  ): monocle.Optional[S, A] =
    monocle.Optional(smithy4sOptional.project)(smithy4sOptional.replace)

}
```

Then you can `import MonocleConversions._` at the top of any file you need to seamlessly convert smithy4s optics over to Monocle ones.
