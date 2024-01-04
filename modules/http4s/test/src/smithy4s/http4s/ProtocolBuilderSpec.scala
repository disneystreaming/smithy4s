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

package smithy4s.http4s

import cats.effect.IO
import org.http4s.HttpApp
import org.http4s.client.Client
import smithy4s.example.PizzaAdminServiceGen
import smithy4s.example.WeatherGen
import weaver._

object ProtocolBuilderSpec extends FunSuite {

  private val fakeClient = Client.fromHttpApp(HttpApp.notFound[IO])

  test(
    "SimpleProtocolBuilder (client) fails when the protocol is not present"
  ) {
    val result = SimpleRestJsonBuilder(WeatherGen)
      .client(fakeClient)
      .make

    assert(result.isLeft)
  }

  test(
    "SimpleProtocolBuilder (client) succeeds when the protocol is present"
  ) {
    val result = SimpleRestJsonBuilder(PizzaAdminServiceGen)
      .client(fakeClient)
      .make

    assert(result.isRight)
  }

}
