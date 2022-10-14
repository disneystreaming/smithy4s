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

package smithy4s.http4s

import cats.effect.IO
import cats.effect.Resource
import org.http4s.HttpApp
import org.http4s.client.Client
import smithy4s.example.PizzaAdminService

object Http4sPizzaClientSpec extends smithy4s.tests.PizzaClientSpec {
  def makeClient: Either[
    HttpApp[IO] => Resource[IO, PizzaAdminService[IO]],
    Int => Resource[IO, PizzaAdminService[IO]]
  ] = Left { httpApp =>
    RestJsonBuilder(PizzaAdminService)
      .client(Client.fromHttpApp(httpApp))
      .resource
  }

  def server(app: HttpApp[IO]): Resource[IO, Int] =
    Resource.eval(IO.raiseError(new Exception("this shouldn't be used")))

}
