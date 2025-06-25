/*
 *  Copyright 2021-2025 Disney Streaming
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

import smithy4s.routing.RoutingService
import cats.effect.IO
import smithy4s.routing.MessageOutput

class RoutingServiceImpl extends RoutingService[IO] {

  override def greedyAbcDef(abc: String): IO[MessageOutput] =
    IO.pure(MessageOutput("greedyAbcDef"))

  override def abcLabel(_def: String): IO[MessageOutput] =
    IO.pure(MessageOutput("abcLabel"))

  override def abcDef(): IO[MessageOutput] = IO.pure(MessageOutput("abcDef"))

  override def abc(): IO[MessageOutput] = IO.pure(MessageOutput("abc"))

}
