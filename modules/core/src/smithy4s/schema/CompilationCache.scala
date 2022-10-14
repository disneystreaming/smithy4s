package smithy4s
package schema
import scala.collection.mutable.{Map => MMap}

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
