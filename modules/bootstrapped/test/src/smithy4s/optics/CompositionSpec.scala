package smithy4s.optics

import munit._
import smithy4s.example._

final class CompositionSpec extends FunSuite {

  test("Lens transformation and composition") {
    val input = EchoInput("test", EchoBody(Some("test body")))
    val lens = EchoInput.Optics.bodyLens.andThen(EchoBody.Optics.dataLens).some
    val resultGet = lens.project(input)

    val resultSet =
      lens.replace("new body")(input)

    val updatedInput = EchoInput("test", EchoBody(Some("new body")))
    assertEquals(Option("test body"), resultGet)
    assertEquals(updatedInput, resultSet)
  }

  test("Prism transformation and composition") {
    val input = Podcast.Video(Some("Pod Title"))

    val prism =
      Podcast.Optics.videoPrism.andThen(Podcast.Video.Optics.titleLens).some
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

    val pizzaName = AddMenuItemRequest.Optics.menuItemLens
      .andThen(MenuItem.Optics.foodLens)
      .andThen(Food.Optics.pizzaPrism)
      .andThen(Pizza.Optics.nameLens)
    val updated = pizzaName.replace("Fancy New Name")(input)

    val result = pizzaName.project(updated)

    assertEquals(Option("Fancy New Name"), result)
  }

  test("enum prisms") {
    val input = Pizza("Cheese", PizzaBase.TOMATO, List(Ingredient.CHEESE))

    val base = Pizza.Optics.baseLens.andThen(PizzaBase.Optics.TOMATOPrism)
    val baseCream = Pizza.Optics.baseLens.andThen(PizzaBase.Optics.CREAMPrism)

    val result = base.project(input)
    val result2 = baseCream.project(input)

    assertEquals(Option(PizzaBase.TOMATO), result)
    assertEquals(Option.empty[PizzaBase.CREAM.type], result2)
  }

  test("lens composition newtypes") {
    val input = GetCityInput(CityId("test"))

    val cityName: Lens[GetCityInput, String] =
      GetCityInput.Optics.cityIdLens.value
    val updated = cityName.replace("Fancy New Name")(input)

    val result = cityName.project(updated)

    assertEquals(Option("Fancy New Name"), result)
  }

  test("prism composition newtypes") {
    val input = PersonContactInfo.EmailCase(PersonEmail("test@test.com"))

    val emailPrism: Prism[PersonContactInfo, String] =
      PersonContactInfo.Optics.emailPrism.value
    val updated = emailPrism.replace("other@other.com")(input)

    val result = emailPrism.project(updated)

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
      topLevel.andThen(PersonContactInfo.Optics.emailPrism).value
    val updated = emailOpt.replace("other@other.com")(input)

    val result = emailOpt.project(updated)

    assertEquals(Option("other@other.com"), result)
  }

}
