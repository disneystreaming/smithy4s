package smithy4s.schema

trait CachedSchemaCompiler[F[_]] {

  type Cache
  def createCache(): Cache
  def fromSchema[A](schema: Schema[A]): F[A]
  def fromSchema[A](schema: Schema[A], cache: Cache): F[A]

}

object CachedSchemaCompiler {

  private[smithy4s] abstract class Impl[F[_]] extends CachedSchemaCompiler[F] {
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
