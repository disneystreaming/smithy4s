/*
 *  Copyright 2021-2023 Disney Streaming
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

import munit.FunSuite
import smithy4s.schema._

class CachedSchemaCompilerSpec() extends FunSuite {

  test(
    "CachedSchemaCompiler.Impl memoizes the result of `fromSchemaAux`"
  ) {
    var x = 0
    val compiler = new CachedSchemaCompiler.Impl[Option] {
      def fromSchemaAux[A](schema: Schema[A], cache: AuxCache): Option[A] = {
        x += 1
        None
      }
    }
    val cache = compiler.createCache()
    val _ = compiler.fromSchema(Schema.int, cache)
    val _ = compiler.fromSchema(Schema.int, cache)
    assertEquals(x, 1)
  }

}
