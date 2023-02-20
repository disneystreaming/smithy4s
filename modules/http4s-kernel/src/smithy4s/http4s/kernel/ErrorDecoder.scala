package smithy4s.http4s.kernel

import smithy4s.Errorable
import smithy4s.schema.SchemaAlt
import smithy4s.kinds.PolyFunction
import smithy4s.kinds.Kind1
import org.http4s.Response
import cats.syntax.all._
import cats.effect.Concurrent
import org.http4s.EntityDecoder
import cats.MonadThrow

trait ErrorDecoder[F[_], E] {

  def decodeError(response: Response[F]): F[E]

  def decodeErrorAsThrowable(response: Response[F]): F[Throwable]

}

object ErrorDecoder {

  def compile[F[_]: Concurrent, E](
      maybeErrorable: Option[Errorable[E]],
      entityCompiler: EntityCompiler[F],
      discriminator: Response[F] => F[Option[SchemaAlt[E, _]]]
  ): ErrorDecoder[F, E] = maybeErrorable match {
    case None            => errorResponseFallBack[F, E]
    case Some(errorable) => compileAux(errorable, entityCompiler, discriminator)
  }

  private def errorResponseFallBack[F[_]: Concurrent, E]: ErrorDecoder[F, E] =
    new ErrorDecoder[F, E] {
      def decodeError(response: Response[F]): F[E] =
        decodeErrorAsThrowable(response).flatMap(MonadThrow[F].raiseError[E](_))

      def decodeErrorAsThrowable(response: Response[F]): F[Throwable] = {
        val headers = getHeaders(response)
        val code = response.status.code
        response.as[String].map { case body =>
          smithy4s.http.UnknownErrorResponse(code, headers, body): Throwable
        }
      }

    }

  private def compileAux[F[_], E](
      errorable: Errorable[E],
      entityCompiler: EntityCompiler[F],
      discriminator: Response[F] => F[Option[SchemaAlt[E, _]]]
  )(implicit F: Concurrent[F]): ErrorDecoder[F, E] = new ErrorDecoder[F, E] {

    def decodeError(
        response: Response[F]
    ): F[E] = {
      response.toStrict(None).flatMap { strictResponse =>
        discriminator(strictResponse).flatMap {
          case None =>
            val code = strictResponse.status.code
            val headers = getHeaders(strictResponse)
            strictResponse.as[String].flatMap { body =>
              F.raiseError(
                smithy4s.http.UnknownErrorResponse(code, headers, body)
              )
            }
          case Some(alt) =>
            cachedDecoders(alt).apply(strictResponse)
        }
      }
    }

    type ConstErrorDecoder[A] = Response[F] => F[E]
    val cachedDecoders: PolyFunction[SchemaAlt[E, *], ConstErrorDecoder] =
      new PolyFunction[SchemaAlt[E, *], ConstErrorDecoder] {
        def apply[A](alt: SchemaAlt[E, A]) = {
          val schema = alt.instance
          // TODO: apply proper memoization of error instances/
          // In the line below, we create a new, ephemeral cache for the dynamic recompilation of the error schema.
          // This is because the "compile entity encoder" method can trigger a transformation of hints, which
          // lead to cache-miss and would lead to new entries in existing cache, effectively leading to a memory leak.
          val ephemeralEntityCache = entityCompiler.createCache()
          implicit val errorCodec: EntityDecoder[F, A] =
            entityCompiler.compileEntityDecoder(schema, ephemeralEntityCache)

          (_: Response[F]).as[A].map(alt.inject)
        }
      }.unsafeCacheBy(
        errorable.error.alternatives.map(Kind1.existential(_)),
        identity(_)
      )

    def decodeErrorAsThrowable(response: Response[F]): F[Throwable] =
      decodeError(response).map(errorable.unliftError)
  }

}
