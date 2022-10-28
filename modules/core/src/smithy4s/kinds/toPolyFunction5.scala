package smithy4s.kinds

object toPolyFunction5 {

  /**
    * Lifts a PolyFunction to a PolyFunction5
    */
  def apply[F[_], G[_]](
      f: PolyFunction[F, G]
  ): PolyFunction5[Kind1[F]#toKind5, Kind1[G]#toKind5] =
    new PolyFunction5[Kind1[F]#toKind5, Kind1[G]#toKind5] {
      def apply[I, E, O, SI, SO](fa: F[O]): G[O] = f(fa)
    }

  /**
    * Lifts a PolyFunction2 to a PolyFunction5
    */
  def apply[F[_, _], G[_, _]](
      f: PolyFunction2[F, G]
  ): PolyFunction5[Kind2[F]#toKind5, Kind2[G]#toKind5] =
    new PolyFunction5[Kind2[F]#toKind5, Kind2[G]#toKind5] {
      def apply[I, E, O, SI, SO](fa: F[E, O]): G[E, O] = f(fa)
    }

}
