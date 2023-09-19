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

import smithy4s.kinds.PolyFunction

trait Errorable[E] extends ShapeTag[E] { self =>
  def schema: _root_.smithy4s.schema.Schema.UnionSchema[E]
  def liftError(throwable: Throwable): Option[E]
  def unliftError(e: E): Throwable

  /**
   * Transforms the local hints of each alternative.
   */
  def transformHintsLocally(f: Hints => Hints): Errorable[E] =
    new Errorable[E] {
      val id = self.id
      val schema = _root_.smithy4s.schema.Schema.UnionSchema(
        self.schema.shapeId,
        self.schema.hints,
        self.schema.alternatives.map(_.transformHintsLocally(f)),
        self.schema.ordinal
      )
      def liftError(throwable: Throwable): Option[E] = self.liftError(throwable)
      def unliftError(e: E): Throwable = self.unliftError(e)
    }

  /**
   * Transforms the hints of each alternative, transitively.
   */
  def transformHintsTransitively(f: Hints => Hints): Errorable[E] =
    new Errorable[E] {
      val id = self.id
      val schema = _root_.smithy4s.schema.Schema.UnionSchema(
        self.schema.shapeId,
        self.schema.hints,
        self.schema.alternatives.map(_.transformHintsTransitively(f)),
        self.schema.ordinal
      )
      def liftError(throwable: Throwable): Option[E] = self.liftError(throwable)
      def unliftError(e: E): Throwable = self.unliftError(e)
    }

}

object Errorable {

  trait Companion[E] extends ShapeTag.Companion[E] with Errorable[E]

  def transformHintsLocallyK(
      f: Hints => Hints
  ): PolyFunction[Errorable, Errorable] =
    new PolyFunction[Errorable, Errorable] {
      def apply[E](errorable: Errorable[E]): Errorable[E] =
        errorable.transformHintsLocally(f)
    }

  def transformHintsTransitivelyK(
      f: Hints => Hints
  ): PolyFunction[Errorable, Errorable] =
    new PolyFunction[Errorable, Errorable] {
      def apply[E](errorable: Errorable[E]): Errorable[E] =
        errorable.transformHintsTransitively(f)
    }

}
