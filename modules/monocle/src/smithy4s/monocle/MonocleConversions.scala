package smithy4s.monocle

object MonocleConversions {

  implicit def smithy4sToMonocleLens[S, A](
      smithy4sLens: smithy4s.optics.Lens[S, A]
  ): monocle.Lens[S, A] =
    monocle.Lens[S, A](smithy4sLens.get)(smithy4sLens.replace(_))

  implicit def smithy4sToMonoclePrism[S, A](
      smithy4sPrism: smithy4s.optics.Prism[S, A]
  ): monocle.Prism[S, A] =
    monocle.Prism(smithy4sPrism.getOption)(smithy4sPrism.project)

  implicit def smithy4sToMonocleOptional[S, A](
      smithy4sOptional: smithy4s.optics.Optional[S, A]
  ): monocle.Optional[S, A] =
    monocle.Optional(smithy4sOptional.getOption)(smithy4sOptional.replace)

}
