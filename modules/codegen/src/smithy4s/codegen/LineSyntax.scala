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

package smithy4s.codegen

import cats.implicits._
import smithy4s.codegen.WithValue.ToLineWithValue

object LineSyntax {
  implicit class LineInterpolator(val sc: StringContext) extends AnyVal {
    def line(renderables: ToLineWithValue[_]*): Line = {
      renderAndCombine(renderables.toList)
    }

    private def renderAndCombine(
        renderables: List[ToLineWithValue[_]]
    ): Line = {
      def aux[A](binding: ToLineWithValue[A]): Line = {
         binding.render
      }
      val renderLines: List[Line] = renderables.map(r => aux(r))
      sc.parts.toList
        .map(Line(_))
        .zipAll(renderLines, Line.empty, Line.empty)
        .flatMap { case (a, b) => List(a, b) }
        .combineAll
    }

  }
}
