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

trait CachedSchemaCompiler[+F[_]] { self =>

  type Cache
  def createCache(): Cache
  def fromSchema[A](schema: Schema[A]): F[A]
  def fromSchema[A](schema: Schema[A], cache: Cache): F[A]

  final def mapK[F0[x] >: F[x], G[_]](
      fk: PolyFunction[F0, G]
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

  final def contramapSchema(
      fk: PolyFunction[Schema, Schema]
  ): CachedSchemaCompiler[F] = new CachedSchemaCompiler[F] {
    type Cache = self.Cache
    def createCache(): Cache = self.createCache()

    def fromSchema[A](schema: Schema[A]): F[A] = self.fromSchema(fk(schema))

    def fromSchema[A](schema: Schema[A], cache: Cache): F[A] =
      self.fromSchema(fk(schema), cache)

  }

}

object CachedSchemaCompiler { outer =>

  type Optional[F[_]] = CachedSchemaCompiler[OptionK[F, *]]
  object Optional {
    abstract class Impl[F[_]] extends outer.Impl[OptionK[F, *]]
  }

  def getOrElse[F[_]](
      possible: CachedSchemaCompiler.Optional[F],
      default: CachedSchemaCompiler[F]
  ): CachedSchemaCompiler[F] = new CachedSchemaCompiler[F] {
    type Cache = (possible.Cache, default.Cache)
    def createCache(): Cache = (possible.createCache(), default.createCache())

    def fromSchema[A](schema: Schema[A]): F[A] =
      possible.fromSchema(schema).getOrElse(default.fromSchema(schema))

    def fromSchema[A](schema: Schema[A], cache: Cache): F[A] = possible
      .fromSchema(schema, cache._1)
      .getOrElse(default.fromSchema(schema, cache._2))

  }

  implicit val cachedSchemaCompilerFunctorK: FunctorK[CachedSchemaCompiler] =
    new FunctorK[CachedSchemaCompiler] {
      def mapK[F[_], G[_]](
          self: CachedSchemaCompiler[F],
          fk: PolyFunction[F, G]
      ): CachedSchemaCompiler[G] =
        self.mapK(fk)
    }

  abstract class Impl[F[_]] extends CachedSchemaCompiler[F] {
    protected type Aux[_]
    type Cache = CompilationCache[Aux]

    override final def fromSchema[A](schema: Schema[A]): F[A] =
      fromSchema(schema, CompilationCache.nop[Aux])

    def createCache(): Cache = CompilationCache.make[Aux]
  }

  abstract class Uncached[F[_]] extends CachedSchemaCompiler[F] {
    type Cache = Any
    def createCache(): Cache = ()
    override final def fromSchema[A](schema: Schema[A], cache: Cache): F[A] =
      fromSchema(schema)
  }

  abstract class DerivingImpl[F[_]] extends Impl[F] {
    private val globalCache: Cache = createCache()
    implicit def derivedImplicitInstance[A](implicit
        schema: Schema[A]
    ): F[A] = {
      fromSchema(schema, globalCache)
    }
  }

}
