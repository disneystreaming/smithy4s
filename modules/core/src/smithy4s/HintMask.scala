/*
 *  Copyright 2021 Disney Streaming
 *
 *  Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     https://disneystreaming.github.io/TOST-1.0.txt
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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
      val hintsToKeep = hints.all.filter(h => toSet.contains(h.key)).toSeq
      Hints(hintsToKeep: _*)
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
