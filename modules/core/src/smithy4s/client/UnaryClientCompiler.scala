package smithy4s.client

import smithy4s.Endpoint
import smithy4s.capability.MonadThrowLike

object UnaryClientCompiler {

  def apply[Alg[_[_, _, _, _, _]], F[_], Client, Request, Response](
      service: smithy4s.Service[Alg],
      client: Client,
      toSmithy4sClient: Client => UnaryLowLevelClient[F, Request, Response],
      makeClientCodecs: UnaryClientCodecs.Make[F, Request, Response],
      middleware: Endpoint.Middleware[Client],
      isSuccessful: Response => Boolean
  )(implicit F: MonadThrowLike[F]): service.FunctorEndpointCompiler[F] =
    new service.FunctorEndpointCompiler[F] {
      def apply[I, E, O, SI, SO](
          endpoint: service.Endpoint[I, E, O, SI, SO]
      ): I => F[O] = {

        val transformedClient =
          middleware.prepare(service)(endpoint).apply(client)

        val adaptedClient = toSmithy4sClient(transformedClient)

        UnaryClientEndpoint(
          adaptedClient,
          makeClientCodecs(endpoint),
          isSuccessful
        )
      }
    }

}
