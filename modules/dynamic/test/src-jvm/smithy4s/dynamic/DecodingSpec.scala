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
package dynamic

import Fixtures._
import DummyIO._
import smithy4s.dynamic.model.Model

class DecodingSpec() extends munit.FunSuite {

  /*
   * Although not needed for equality, we sort the shapes in the model
   * to make diffs more readable in case of failed assertions.
   */
  private def order(model: Model): Model =
    model.copy(shapes =
      scala.collection.immutable.ListMap(
        model.shapes.toSeq.sortBy(_._1.value): _*
      )
    )

  test("Decode json representation of models") {
    val expected = order(pizzaModel)
    Utils
      .parse(pizzaModelString)
      .map(order)
      .mapRun(obtained => expect.same(obtained, expected))
  }

}
