package smithy4s

// BINCOMPAT FOR 0.18 START
trait StaticBindingPlatform[A] { self: Hints.Binding.StaticBinding[A] =>
  private[smithy4s] def copy$default$1(): ShapeTag[A] = k
}
// BINCOMPAT FOR 0.18 END
