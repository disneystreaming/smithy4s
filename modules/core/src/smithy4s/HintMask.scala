/*
 *  Copyright 2021-2022 Disney Streaming
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

sealed abstract class HintMask {
  def ++(other: HintMask): HintMask
  def apply(hints: Hints): Hints
}

object HintMask {

  def allAllowed: HintMask = Permissive

  def empty: HintMask = apply()

  def apply(traits: Set[ShapeId]): HintMask = {
    new Impl(traits)
  }

  def apply(shapeTags: ShapeTag[_]*): HintMask = {
    new Impl(shapeTags.map(_.id).toSet)
  }

  private[this] case object Permissive extends HintMask {
    def ++(other: HintMask): HintMask = this
    def apply(hints: Hints): Hints = hints
  }

  private[this] final class Impl(val shapeIds: Set[ShapeId]) extends HintMask {
    def ++(other: HintMask): HintMask = other match {
      case i: Impl    => new Impl(shapeIds ++ i.shapeIds)
      case Permissive => Permissive
    }

    def apply(hints: Hints): Hints = {
      val hintsToKeep =
        hints.all.filter(h => shapeIds.contains(h.keyId)).toSeq
      Hints(hintsToKeep: _*)
    }
  }

}
