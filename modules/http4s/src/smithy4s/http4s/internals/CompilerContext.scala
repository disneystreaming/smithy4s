package smithy4s.http4s.internals

import smithy4s.http4s.EntityCompiler
import smithy4s.http.Metadata

private[http4s] trait CompilerContext[F[_]] {
  val entityCompiler: EntityCompiler[F]
  val entityCache: entityCompiler.Cache
  val metadataDecoderCache: Metadata.PartialDecoder.Cache
  val metadataEncoderCache: Metadata.PartialDecoder.Cache
}

private[http4s] object CompilerContext {

  def make[F[_]](ec: EntityCompiler[F]): CompilerContext[F] =
    new CompilerContext[F] {
      val entityCompiler: EntityCompiler[F] = ec
      val entityCache: entityCompiler.Cache = entityCompiler.createCache()
      val metadataDecoderCache: Metadata.PartialDecoder.Cache =
        Metadata.PartialDecoder.createCache()
      val metadataEncoderCache: Metadata.PartialDecoder.Cache =
        Metadata.PartialDecoder.createCache()

    }

}
