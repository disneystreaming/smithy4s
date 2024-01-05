/*
 *  Copyright 2021-2024 Disney Streaming
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
package schema
import smithy4s.internals.maps.MMap

trait CompilationCache[F[_]] {
  def getOrElseUpdate[A](schema: Schema[A], fetch: Schema[A] => F[A]): F[A]
}

object CompilationCache {

  /**
    * A no-op cache that doesn't actually cache anything but just forwards queries
    * to the fetch function.
    */
  def nop[F[_]]: CompilationCache[F] = new CompilationCache[F] {
    override def getOrElseUpdate[A](
        schema: Schema[A],
        fetch: Schema[A] => F[A]
    ): F[A] =
      fetch(schema)
  }

  /**
    * Creates a compilation cache that can be used to speed up the initialisation
    * of interpreters and reduce their memory footprint by sharing entities that have
    * the same Schemas.
    *
    * This should be used with care, as reckless usage can lead to memory leaks, since
    * Schemas are not guaranteed to have stable hashCodes/equality methods when re-instantiated.
    */
  def make[F[_]]: CompilationCache[F] = new CompilationCache[F] {
    private val store: MMap[Any, Any] = MMap.empty

    override def getOrElseUpdate[A](
        schema: Schema[A],
        fetch: Schema[A] => F[A]
    ): F[A] = {
      // Lazy is tricky in that the thunk it contains can never be expressed
      // in a "stable" way, even in a dynamic context when most accessors/injectors can be
      // expressed in a serialisable fashion.
      if (schema.isInstanceOf[Schema.LazySchema[_]]) { fetch(schema) }
      else store.getOrElseUpdate(schema, fetch(schema)).asInstanceOf[F[A]]
    }
  }

}
