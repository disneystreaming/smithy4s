package smithy4s.server

import smithy4s.Endpoint
import smithy4s.capability.MonadThrowLike

// scalafmt: {maxColumn = 120}
final class UnaryServerCodecs[F[_], Request, Response, I, E, O](
    val inputDecoder: Request => F[I],
    val errorEncoder: E => F[Response],
    val throwableEncoder: Throwable => F[Response],
    val outputEncoder: O => F[Response]
) {

  def transformRequest[Request0](f: Request0 => F[Request])(implicit
      F: MonadThrowLike[F]
  ): UnaryServerCodecs[F, Request0, Response, I, E, O] = {
    new UnaryServerCodecs(
      f.andThen(F.flatMap(_)(inputDecoder)),
      errorEncoder,
      throwableEncoder,
      outputEncoder
    )
  }

  def transformResponse[Response1](
      f: Response => F[Response1]
  )(implicit F: MonadThrowLike[F]): UnaryServerCodecs[F, Request, Response1, I, E, O] = {
    new UnaryServerCodecs(
      inputDecoder,
      errorEncoder.andThen(F.flatMap(_)(f)),
      throwableEncoder.andThen(F.flatMap(_)(f)),
      outputEncoder.andThen(F.flatMap(_)(f))
    )
  }

}

object UnaryServerCodecs {

  type For[F[_], Request, Response] = {
    type toKind5[I, E, O, SI, SO] =
      UnaryServerCodecs[F, Request, Response, I, E, O]
  }

  type Make[F[_], Request, Response] =
    smithy4s.kinds.PolyFunction5[Endpoint.Base, For[F, Request, Response]#toKind5]

}
