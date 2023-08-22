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

package smithy4s.codegen.internals

import cats.syntax.all._
import smithy4s.codegen.internals.LineSegment.Literal

private[internals] class PartialBlock(l: Line, sameLine: Line = Line.empty) {
  def apply[A](inner: A)(implicit A: ToLines[A]): Lines = {
    A.render(inner)
      .transformLines(lines =>
        (l + Literal(" {") + sameLine) :: indent(lines) ::: List(Line("}"))
      )
  }

  def withSameLineValue(value: Line): PartialBlock =
    new PartialBlock(l, value)

  def apply(inner: LinesWithValue*): Lines =
    apply(inner.toList.foldMap(_.render))

}
