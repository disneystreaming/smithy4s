package smithy4s.grpc

import smithy4s.schema._
import smithy4s.capability.Covariant
import smithy4s.kinds.PolyFunction
import smithy4s.ShapeId

object GrpcErrorSelector {
  def asThrowable[F[_]: Covariant, E](
    maybeErrorSchema: Option[ErrorSchema[E]],
    compiler: CachedSchemaCompiler[F]
  ): GrpcDiscriminator => Option[F[Throwable]]= maybeErrorSchema match {
    case Some(errorSchema) => 
      new GrpcErrorSelector[F, E](
        errorSchema.alternatives,
        compiler
      ).andThen(_.map(Covariant[F].map(_)(errorSchema.unliftError)))
    case None => _ => None
  }

}

private[grpc] final class GrpcErrorSelector[F[_]: Covariant, E](
    alts: Vector[Alt[E, _]],
    compiler: CachedSchemaCompiler[F]
) extends (GrpcDiscriminator => Option[F[E]]) {

  type ConstF[A] = F[E]
  val cachedDecoders: PolyFunction[Alt[E, *], ConstF] =
    new PolyFunction[Alt[E, *], ConstF] {
      def compileAlt[A](alt: Alt[E, A]): F[E] = {
        val schema = alt.schema
        // In the line below, we create a new, ephemeral cache for the dynamic recompilation of the error schema.
        // This is because the "compile body encoder" method can trigger a transformation of hints, which
        // lead to cache-miss and would lead to new entries in existing cache, effectively leading to a memory leak.
        val cache = compiler.createCache()
        val errorCodec: F[A] = compiler.fromSchema(schema, cache)
        Covariant[F].map[A, E](errorCodec)(alt.inject)
      }
      val builder = Map.newBuilder[Any, Any]
      alts.foreach { alt =>
        builder += alt -> compileAlt(alt)
      }
      val resultCache = builder.result()
      def apply[A](alt: Alt[E, A]): F[E] = {
        resultCache(alt).asInstanceOf[F[E]]
      }
    }

  def apply(discriminator: GrpcDiscriminator): Option[F[E]] = {
    val alt = getPreciseAlternative(discriminator)
    alt.map(cachedDecoders(_))
  }

  private val byShapeId: ShapeId => Option[Alt[E, _]] = {
    val perShapeId: Map[ShapeId, Alt[E, _]] = 
      alts
        .map(alt => alt.schema.shapeId -> alt)
        .toMap

    val errorForShapeId: ShapeId => Option[Alt[E, _]] = perShapeId.get

    shapeId =>
      errorForShapeId(shapeId)
  }

  private[grpc] def getPreciseAlternative(
      discriminator: GrpcDiscriminator
  ): Option[Alt[E, _]] = {
    import GrpcDiscriminator._
    discriminator match {
      case ByShapeId(shapeId) => byShapeId(shapeId)
      case Undetermined => None
    }
  }
}
