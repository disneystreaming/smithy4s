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

  test("enum prisms") {
    val input = Pizza("Cheese", PizzaBase.TOMATO, List(Ingredient.CHEESE))

    val base = Pizza.Optics.base.andThen(PizzaBase.Optics.TOMATO)
    val baseCream = Pizza.Optics.base.andThen(PizzaBase.Optics.CREAM)

    val result = base.getOption(input)
    val result2 = baseCream.getOption(input)

    assertEquals(Option(PizzaBase.TOMATO), result)
    assertEquals(Option.empty[PizzaBase.CREAM.type], result2)
  }

  test("lens composition newtypes") {
    val input = GetCityInput(CityId("test"))

    val cityName: Lens[GetCityInput, String] = GetCityInput.Optics.cityId.value
    val updated = cityName.replace("Fancy New Name")(input)

    val result = cityName.getOption(updated)

    assertEquals(Option("Fancy New Name"), result)
  }

  test("prism composition newtypes") {
    val input = PersonContactInfo.EmailCase(PersonEmail("test@test.com"))

    val emailPrism: Prism[PersonContactInfo, String] =
      PersonContactInfo.Optics.email.value
    val updated = emailPrism.replace("other@other.com")(input)

    val result = emailPrism.getOption(updated)

    assertEquals(Option("other@other.com"), result)
  }

  test("optional composition newtypes") {
    case class TopLevel(contact: PersonContactInfo)
    val topLevel = Lens[TopLevel, PersonContactInfo](_.contact)(a =>
      s => s.copy(contact = a)
    )
    val input =
      TopLevel(PersonContactInfo.EmailCase(PersonEmail("test@test.com")))

    val emailOpt: Optional[TopLevel, String] =
      topLevel.andThen(PersonContactInfo.Optics.email.value)
    val updated = emailOpt.replace("other@other.com")(input)

    val result = emailOpt.getOption(updated)

    assertEquals(Option("other@other.com"), result)
  }

}
