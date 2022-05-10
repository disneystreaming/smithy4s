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

package smithy4s.http4s.swagger

import cats.effect.Blocker
import cats.effect.IO
import smithy4s.HasId
import smithy4s.ShapeId
import weaver._

import scala.concurrent.ExecutionContext

trait TestCompat { self: BaseIOSuite with RunnableSuite[IO] =>

  def blocker: Blocker =
    Blocker.liftExecutionContext(ExecutionContext.Implicits.global)

  def service = new HasId {
    def id: ShapeId = ShapeId("foobar", "test-spec")
  }

  def docs(path: String) =
    Docs[IO](service, blocker, path, swaggerUiPath = "swaggerui")
}
