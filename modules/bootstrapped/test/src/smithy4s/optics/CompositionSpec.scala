/*
 *  Copyright 2021-2024 Disney Streaming
 *
 *  Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     https://disneystreaming.github.io/TOST-1.0.txt
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package smithy4s.optics

import munit._
import smithy4s.example._

final class CompositionSpec extends FunSuite {

  test("Lens transformation and composition") {
    val input = TestInput("test", TestBody(Some("test body")))
    val lens =
      TestInput.optics.body.andThen(TestBody.optics.data).some[String]
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
      Podcast.optics.video
        .andThen(Podcast.Video.optics.title)
        .some[String]
    val result = prism.replace("New Pod Title")(input)

    assertEquals(Podcast.Video(Some("New Pod Title")).widen, result)
  }

  test("nested") {
    val input = GetForecastOutput(
      Some(ForecastResult.SunCase(UVIndex(6)))
    )

    val uvIndex = GetForecastOutput.optics.forecast
      .some[ForecastResult]
      .andThen(ForecastResult.optics.sun)
      .value
    val updated = uvIndex.replace(8)(input)

    val result = uvIndex.project(updated)

    assertEquals(Option(8), result)
  }

  test("enum prisms") {
    val input = OpticsStructure(Some(OpticsEnum.A))

    val base =
      OpticsStructure.optics.two
        .some[OpticsEnum]
        .andThen(OpticsEnum.optics.A)
    val baseB =
      OpticsStructure.optics.two
        .some[OpticsEnum]
        .andThen(OpticsEnum.optics.B)

    val result = base.project(input)
    val result2 = baseB.project(input)

    assertEquals(Option(OpticsEnum.A), result)
    assertEquals(Option.empty[OpticsEnum.B.type], result2)
  }

  test("lens composition newtypes") {
    val input = GetCityInput(CityId("test"))

    val cityName: Lens[GetCityInput, String] =
      GetCityInput.optics.cityId.value
    val updated = cityName.replace("Fancy New Name")(input)

    val result = cityName.project(updated)

    assertEquals(Option("Fancy New Name"), result)
  }

  test("prism composition newtypes") {
    val input = PersonContactInfo.EmailCase(PersonEmail("test@test.com"))

    val emailPrism: Prism[PersonContactInfo, String] =
      PersonContactInfo.optics.email.value
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
      topLevel.andThen(PersonContactInfo.optics.email).value
    val updated = emailOpt.replace("other@other.com")(input)

    val result = emailOpt.project(updated)

    assertEquals(Option("other@other.com"), result)
  }

}
