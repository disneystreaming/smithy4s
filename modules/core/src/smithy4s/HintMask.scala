package smithy4s

abstract class HintMask {
  protected def toSet: Set[Hints.Key[_]]
  def ++(other: HintMask): HintMask
  def apply(hints: Hints): Hints
}

object HintMask {
  def empty: HintMask = apply()

  def apply(hintKeys: Hints.Key[_]*): HintMask = {
    new Impl(hintKeys.toSet)
  }

  private[this] final class Impl(val toSet: Set[Hints.Key[_]])
      extends HintMask {
    def ++(other: HintMask): HintMask =
      new Impl(toSet ++ other.toSet)
    def apply(hints: Hints): Hints = {
      val hintKeysToRemove = hints.toMap.keySet.diff(toSet)
      hintKeysToRemove.foldLeft(hints)((all, key) => all.remove(key))
    }
  }

  private[this] final class MaskSchematic[F[_]](
      schematic: Schematic[F],
      mask: HintMask
  ) extends PassthroughSchematic[F](schematic) {
    override def withHints[A](fa: F[A], hints: Hints): F[A] =
      schematic.withHints(fa, mask(hints))
  }

  def mask[F[_]](schematic: Schematic[F], mask: HintMask): Schematic[F] =
    new MaskSchematic[F](schematic, mask)
}
