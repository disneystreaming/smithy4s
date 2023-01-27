/*
 *  Copyright 2021-2022 Disney Streaming
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

package smithy4s.tests

import cats.effect._
import cats.effect.std.UUIDGen
import cats.implicits._
import smithy4s.Timestamp
import smithy4s.example._

import java.util.UUID

import PizzaAdminServiceImpl._

object PizzaAdminServiceImpl {
  case class Item(food: Food, price: Float, addedAt: Timestamp)
  case class State(restaurants: Map[String, Restaurant])
  case class Restaurant(menu: Map[UUID, Item])

  case object Boom extends Throwable with scala.util.control.NoStackTrace
}

class PizzaAdminServiceImpl(ref: Compat.Ref[IO, State])
    extends PizzaAdminService[IO] {

  def book(name: String, town: Option[String]): IO[BookOutput] =
    IO.pure(BookOutput(message = s"Booked for $name"))

  def getEnum(theEnum: TheEnum): IO[GetEnumOutput] =
    IO.pure(GetEnumOutput(result = Some(theEnum.value)))

  def getIntEnum(theEnum: EnumResult): IO[GetIntEnumOutput] =
    IO.pure(GetIntEnumOutput(theEnum))

  def addMenuItem(
      restaurant: String,
      menuItem: MenuItem
  ): IO[AddMenuItemResult] =
    for {
      _ <- IO.raiseError(Boom).whenA(restaurant == "boom")
      _ <- IO
        .raiseError(
          PriceError(
            s"Prices must be whole numbers: ${menuItem.price}",
            code = 1
          )
        )
        .unlessA(menuItem.price.isWhole)
      uuid <- UUIDGen.randomUUID[IO]
      timestamp <- IO(Timestamp.nowUTC())
      _ <- ref.update { state =>
        val item = Item(menuItem.food, menuItem.price, timestamp)
        val restau = state.restaurants
          .get(restaurant)
          .map(r => r.copy(menu = r.menu + (uuid -> item)))
          .getOrElse(Restaurant(Map(uuid -> item)))
        state.copy(restaurants = state.restaurants + (restaurant -> restau))
      }
    } yield AddMenuItemResult(uuid.toString(), timestamp)

  def getMenu(restaurant: String): IO[GetMenuResult] =
    for {
      state <- ref.get
      map <- state.restaurants
        .get(restaurant)
        .map(_.menu.map { case (key, item) =>
          key.toString -> MenuItem(item.food, item.price)
        })
        .liftTo[IO](NotFoundError(restaurant))
    } yield GetMenuResult(map)

  def version(): IO[VersionOutput] = IO.pure(VersionOutput("version"))

  def health(query: Option[String]): IO[HealthResponse] =
    IO.pure(HealthResponse(query.getOrElse("empty")))

  def headerEndpoint(
      uppercaseHeader: Option[String],
      capitalizedHeader: Option[String],
      lowercaseHeader: Option[String],
      mixedHeader: Option[String]
  ): IO[HeaderEndpointData] = HeaderEndpointData(
    uppercaseHeader = uppercaseHeader,
    capitalizedHeader = capitalizedHeader,
    lowercaseHeader = lowercaseHeader,
    mixedHeader = mixedHeader
  ).pure[IO]

  def roundTrip(
      label: String,
      header: Option[String] = None,
      query: Option[String] = None,
      body: Option[String] = None
  ): IO[RoundTripData] = IO.pure(
    RoundTripData(label, header, query, body)
  )

  def customCode(code: Int): IO[CustomCodeOutput] =
    IO.pure(CustomCodeOutput(if (code != 0) Some(code) else None))

  def echo(
      pathParam: String,
      body: EchoBody,
      queryParam: Option[String]
  ): IO[Unit] = IO.unit
}
