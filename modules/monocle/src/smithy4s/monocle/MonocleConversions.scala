package smithy4s.monocle

object MonocleConversions {

  implicit def smithy4sToMonocleLens[S, A](
      smithy4sLens: smithy4s.optics.Lens[S, A]
  ): monocle.Lens[S, A] =
    monocle.Lens[S, A](smithy4sLens.get)(smithy4sLens.replace(_))

}
