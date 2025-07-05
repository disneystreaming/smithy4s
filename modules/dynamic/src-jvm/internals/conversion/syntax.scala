/*
 *  Copyright 2021-2025 Disney Streaming
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

package smithy4s.dynamic.internals.conversion

import smithy4s.{ShapeId, Hints}
import software.amazon.smithy.model.shapes.{AbstractShapeBuilder, Shape}
import software.amazon.smithy.utils.ToSmithyBuilder
import software.amazon.smithy.model.traits.Trait
import smithy4s.dynamic.syntax._

private[dynamic] object syntax {
  implicit class ShapeBuilderOps[A <: AbstractShapeBuilder[A, S], S <: Shape](
      builder: AbstractShapeBuilder[A, S]
  ) {
    def setId(sid: ShapeId): A = {
      builder.id(sid.toSmithy)
      builder.asInstanceOf[A]
    }
  }

  implicit class ShapeOps[A <: Shape](val a: A) extends AnyVal {
    def captureHints(hints: Hints): A = addTraits(a, hints)
  }

  def addTraits[A <: Shape](shape: A, hints: Hints): A = {
    shape match {
      case s: ToSmithyBuilder[_] =>
        s.toBuilder match {
          case s: AbstractShapeBuilder[_, _] =>
            s.addTraits(hints.asTraits)
            s.build().asInstanceOf[A]
          case _ => shape
        }
      case _ => shape
    }
  }
}
