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

package smithy4s.schema

import smithy4s.kinds._

trait CachedSchemaCompiler[+F[_]] {

  type Cache
  def createCache(): Cache
  def fromSchema[A](schema: Schema[A]): F[A]
  def fromSchema[A](schema: Schema[A], cache: Cache): F[A]

}

object CachedSchemaCompiler {

  implicit val cachedSchemaCompilerFunctorK: FunctorK[CachedSchemaCompiler] =
    new FunctorK[CachedSchemaCompiler] {
      def mapK[F[_], G[_]](
          self: CachedSchemaCompiler[F],
          fk: PolyFunction[F, G]
      ): CachedSchemaCompiler[G] =
        new CachedSchemaCompiler[G] {
          type Cache = self.Cache
          def createCache(): self.Cache = self.createCache()
          def fromSchema[A](schema: Schema[A]): G[A] = fk(
            self.fromSchema(schema)
          )
          def fromSchema[A](schema: Schema[A], cache: Cache): G[A] = fk(
            self.fromSchema(schema, cache)
          )
        }
    }

  abstract class Impl[F[_]] extends CachedSchemaCompiler[F] {
    protected type Aux[_]
    type Cache = CompilationCache[Aux]

    override final def fromSchema[A](schema: Schema[A]): F[A] =
      fromSchema(schema, CompilationCache.nop[Aux])

    def createCache(): Cache = CompilationCache.make[Aux]

    private val globalCache: Cache = createCache()
    implicit def derivedImplicitInstance[A](implicit
        schema: Schema[A]
    ): F[A] = {
      fromSchema(schema, globalCache)
    }
  }

}
