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

package smithy4s.codegen.internals

import munit.Assertions
import munit.Location
import software.amazon.smithy.model.Model

object TestUtils {

  def runTest(
      smithySpec: String,
      expectedScalaCode: String
  )(implicit
      loc: Location
  ): Unit = {
    val model = Model
      .assembler()
      .discoverModels()
      .addUnparsedModel("foo.smithy", smithySpec)
      .assemble()
      .unwrap()

    val results = CodegenImpl.generate(model, None, None)
    val scalaResults = results.map(_._2.content)
    Assertions.assertEquals(scalaResults, List(expectedScalaCode))
  }

}
