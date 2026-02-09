package smithy4s.grpc

import smithy4s.schema._
import smithy4s.capability.Covariant
import smithy4s.kinds.PolyFunction

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

  private[grpc] def getPreciseAlternative(
      discriminator: GrpcDiscriminator
  ): Option[Alt[E, _]] = {
    import GrpcDiscriminator._
    discriminator match {
      // case FullId(shapeId) => byShapeId.get(shapeId)
      // case NameOnly(name)  => byName.get(name)
      case StatusCode(int) => byStatusCode(int)
      // case Undetermined    => None
    }
  }


  // exclude all status code that are used on multiple alternative
  // in essence, it gives a `Map[Int, Alt[E, _]]` that's used
  // for the lookup
  private val byStatusCode: Int => Option[Alt[E, _]] = {
    //FIXME: we need a way to specify gRPC error codes in smithy
    // Something similar to how the smithy.api.httpError trait works.
    //
    val perStatusCode: Map[Int, Alt[E, _]] = alts
      .flatMap { alt =>
        alt.hints.get(smithy.api.HttpError).map { he => he.value -> alt }
      }
      .groupBy(_._1)
      .collect {
        // Discard alternative where another alternative has the same http status code
        case (status, allAlts) if allAlts.size == 1 => status -> allAlts.head._2
      }
      .toMap
    val errorForStatus: Int => Option[Alt[E, _]] = perStatusCode.get

    // lazy val fallbackError: Int => Option[Alt[E, _]] = {
    //   // grab the alt that's annotated with the expected `Error` hint
    //   // only if there is only one
    //   def forErrorType(expected: Error): Option[Alt[E, _]] = {
    //     val matchingAlts = alts
    //       .flatMap { alt =>
    //         val foo = alt.hints
    //           .get(smithy.api.HttpError)
    //         foo
    //           .fold(
    //             alt.hints.get(Error).collect {
    //               case e if e == expected => alt
    //             }
    //           )(_ => None)

    //       }
    //     if (matchingAlts.size == 1) matchingAlts.headOption else None
    //   }
    //   val clientAlt: Option[Alt[E, _]] = forErrorType(Error.CLIENT)
    //   val serverAlt: Option[Alt[E, _]] = forErrorType(Error.SERVER)

    //   { intStatus =>
    //     if (intStatus >= 400 && intStatus < 500) clientAlt
    //     else if (intStatus >= 500 && intStatus < 600) serverAlt
    //     else None
    //   }
    // }

    inputStatus =>
      errorForStatus(inputStatus)
  }
}
