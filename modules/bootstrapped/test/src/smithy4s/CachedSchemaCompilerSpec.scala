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
    discardResult(compiler.fromSchema(Schema.int, cache))
    discardResult(compiler.fromSchema(Schema.int, cache))
    assertEquals(x, 1)
  }

  test(
    "CachedSchemaCompiler.Impl memoization is stable through mapK"
  ) {
    var x = 0
    val transformation = new smithy4s.kinds.PolyFunction[Option, Option] {
      def apply[A](fa: Option[A]) = {
        x += 1
        fa
      }
    }
    val compiler = new CachedSchemaCompiler.Impl[Option] {
      def fromSchemaAux[A](schema: Schema[A], cache: AuxCache): Option[A] = {
        None
      }
    }.mapK(transformation)
    val cache = compiler.createCache()
    discardResult(compiler.fromSchema(Schema.int, cache))
    discardResult(compiler.fromSchema(Schema.int, cache))
    assertEquals(x, 1)
  }

  test(
    "CachedSchemaCompiler.Impl memoization is stable through contramapSchema"
  ) {
    var x = 0
    val transformation = new smithy4s.kinds.PolyFunction[Schema, Schema] {
      def apply[A](fa: Schema[A]) = {
        x += 1
        fa
      }
    }
    val compiler = new CachedSchemaCompiler.Impl[Option] {
      def fromSchemaAux[A](schema: Schema[A], cache: AuxCache): Option[A] = {
        None
      }
    }.contramapSchema(transformation)
    val cache = compiler.createCache()
    discardResult(compiler.fromSchema(Schema.int, cache))
    discardResult(compiler.fromSchema(Schema.int, cache))
    assertEquals(x, 1)
  }

  private def discardResult[A](f: => A): Unit = {
    val _ = f
  }

}
