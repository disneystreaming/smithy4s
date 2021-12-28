/*
 *  Copyright 2021 Disney Streaming
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

package smithy4s

import weaver._
import smithy4s.example._
import cats.Functor
import cats.syntax.all._
import cats.effect.IO

object TypeInferenceSmokeSpec extends SimpleIOSuite {

  test("Type inference works with service calls") {
    /*
     * Checks that `map` can be called without upcasting the result of
     * the service call to F[something].
     */
    def foo[F[_]: Functor](dummyService: DummyService[F]): F[Int] =
      dummyService.dummy().map(_ => 1)

    val dummyInstance = new DummyService[IO] {

      override def dummy(
          str: Option[String],
          int: Option[Int],
          ts1: Option[Timestamp],
          ts2: Option[Timestamp],
          ts3: Option[Timestamp],
          ts4: Option[Timestamp],
          b: Option[Boolean],
          sl: Option[List[String]],
          slm: Option[Map[String, List[String]]]
      ): IO[Unit] = IO.unit

    }
    foo(dummyInstance).map(x => expect(x == 1))
  }

}
