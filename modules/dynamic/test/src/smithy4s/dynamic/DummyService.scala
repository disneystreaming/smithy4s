package smithy4s
package dynamic

import cats.Applicative

object DummyService {

  def apply[F[_]]: PartiallyApplied[F] = new PartiallyApplied[F]

  class PartiallyApplied[F[_]] {
    def create[Alg[_[_, _, _, _, _]], Op[_, _, _, _, _]](implicit
        service: Service[Alg, Op],
        F: Applicative[F]
    ): smithy4s.Monadic[Alg, F] = {
      service.transform {
        service.opToEndpoint.andThen(
          new Transformation[Endpoint[Op, *, *, *, *, *], GenLift[F]#λ] {
            def apply[I, E, O, SI, SO](
                ep: Endpoint[Op, I, E, O, SI, SO]
            ): F[O] =
              F.pure(ep.output.compile(DefaultSchematic))
          }
        )
      }
    }
  }

}
