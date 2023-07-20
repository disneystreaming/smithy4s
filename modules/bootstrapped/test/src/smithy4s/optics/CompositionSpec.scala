package smithy4s.optics

import munit._
import smithy4s.example._

final class CompositionSpec extends FunSuite {

  test("Lens transformation and composition") {
    val input = TestInput("test", TestBody(Some("test body")))
    val lens =
      TestInput.Optics.bodyLens.andThen(TestBody.Optics.dataLens).some[String]
    val resultGet = lens.project(input)

    val resultSet =
      lens.replace("new body")(input)

    val updatedInput = TestInput("test", TestBody(Some("new body")))
    assertEquals(Option("test body"), resultGet)
    assertEquals(updatedInput, resultSet)
  }

  test("Prism transformation and composition") {
    val input = Podcast.Video(Some("Pod Title"))

    val prism =
      Podcast.Optics.videoPrism
        .andThen(Podcast.Video.Optics.titleLens)
        .some[String]
    val result = prism.replace("New Pod Title")(input)

    assertEquals(Podcast.Video(Some("New Pod Title")).widen, result)
  }

  test("nested") {
    val input = GetForecastOutput(
      Some(ForecastResult.SunCase(UVIndex(6)))
    )

    val uvIndex = GetForecastOutput.Optics.forecastLens
      .some[ForecastResult]
      .andThen(ForecastResult.Optics.sunPrism)
      .value
    val updated = uvIndex.replace(8)(input)

    val result = uvIndex.project(updated)

    assertEquals(Option(8), result)
  }

  test("enum prisms") {
    val input = OpticsStructure(Some(OpticsEnum.A))

    val base =
      OpticsStructure.Optics.twoLens
        .some[OpticsEnum]
        .andThen(OpticsEnum.Optics.APrism)
    val baseB =
      OpticsStructure.Optics.twoLens
        .some[OpticsEnum]
        .andThen(OpticsEnum.Optics.BPrism)

    val result = base.project(input)
    val result2 = baseB.project(input)

    assertEquals(Option(OpticsEnum.A), result)
    assertEquals(Option.empty[OpticsEnum.B.type], result2)
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
