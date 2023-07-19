package smithy4s.optics

import munit._
import smithy4s.example._

final class CompositionSpec extends FunSuite {

  test("Lens transformation and composition") {
    val input = EchoInput("test", EchoBody(Some("test body")))
    val lens = EchoInput.Optics.body.andThen(EchoBody.Optics.data).some
    val resultGet = lens.getOption(input)

    val resultSet =
      lens.replace("new body")(input)

    val updatedInput = EchoInput("test", EchoBody(Some("new body")))
    assertEquals(Option("test body"), resultGet)
    assertEquals(updatedInput, resultSet)
  }

  test("Prism transformation and composition") {
    val input = Podcast.Video(Some("Pod Title"))

    val prism = Podcast.Optics.video.andThen(Podcast.Video.Optics.title).some
    val result = prism.replace("New Pod Title")(input)

    assertEquals(Podcast.Video(Some("New Pod Title")).widen, result)
  }

  test("Deeply Nested") {
    val input = AddMenuItemRequest(
      "Pizza Place",
      MenuItem(
        Food.PizzaCase(
          Pizza("Cheese", PizzaBase.TOMATO, List(Ingredient.CHEESE))
        ),
        price = 12.5f
      )
    )

    val pizzaName = AddMenuItemRequest.Optics.menuItem
      .andThen(MenuItem.Optics.food)
      .andThen(Food.Optics.pizza)
      .andThen(Pizza.Optics.name)
    val updated = pizzaName.replace("Fancy New Name")(input)

    val result = pizzaName.getOption(updated)

    assertEquals(Option("Fancy New Name"), result)
  }

}
