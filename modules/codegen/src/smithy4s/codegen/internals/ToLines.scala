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

import cats.kernel.Monoid
import cats.syntax.all._

import LineSegment.{Literal, NameRef}
import cats.Foldable

/**
  * Construct allowing to flatten arbitrary levels of nested lists
  */
private[internals] trait ToLines[A] {
  def render(a: A): Lines
}

private[internals] object ToLines {

  def render[A](a: A)(implicit A: ToLines[A]): Lines = A.render(a)
  implicit val identity: ToLines[Lines] = r => r

  implicit def foldableRenderable[L[_]: Foldable, A](implicit
      A: ToLines[A]
  ): ToLines[L[A]] = { (l: L[A]) =>
    val lines: List[List[Line]] = l.toList.map(A.render).map(r => r.list)
    Lines(lines.flatten)
  }

  implicit def tupleRenderable[A](implicit
      A: ToLines[A]
  ): ToLines[(String, A)] = (t: (String, A)) => A.render(t._2).addImport(t._1)

  implicit def lineToLines[A: ToLine]: ToLines[A] = (a: A) => {
    val line = ToLine[A].render(a)
    // empty string must be treated like an empty list which is a Monoid Empty as oposed to wrapping in a singleton list which will render a new line character`
    if (line.segments.isEmpty) Lines.empty else Lines(line)
  }

}

// Models

private[internals] case class Lines(list: List[Line]) {
  def isEmpty: Boolean = list.isEmpty

  def block(l: LinesWithValue*): Lines = {
    val openBlock: List[Line] =
      list.lastOption.flatMap(_.segments.lastOption).collect {
        case hardcoded: Literal =>
          hardcoded.value match {
            case ")"   => Line("){")
            case "}"   => Line("}{")
            case other => Line(other + " {")
          }
      } match {
        case Some(value) => list.dropRight(1) :+ value
        case None        => list
      }

    Lines(openBlock) ++ indent(
      l.toList.foldMap(_.render)
    ) ++ Lines("}")
  }

  def appendToLast(s: String): Lines = {
    appendToLast(Line(s))
  }

  def appendToLast(line: Line): Lines = {
    val newLines = list.lastOption.map(_ + line) match {
      case Some(value) => list.dropRight(1) :+ value
      case None        => list
    }
    Lines(newLines)
  }

  def transformLines(f: List[Line] => List[Line]): Lines = Lines(f(list))
  def mapLines(f: Line => Line): Lines = transformLines(_.map(f))

  def ++(other: Lines): Lines =
    Lines(list ++ other.list)

  def addImport(im: String): Lines =
    if (im.nonEmpty) Lines(list :+ NameRef(im).toLine) else this
  def addImports(im: Set[String]): Lines =
    Lines(list ::: im.map(s => NameRef(s).toLine).toList)

  def when(cond: => Boolean): Lines =
    if (cond) this else Lines.empty
}
private[internals] object Lines {
  def apply(line: Line): Lines = Lines(List(line))
  def apply(str: String): Lines = Lines(List(Line(str)))
  val empty = Lines(List.empty[Line])
  implicit val linesMonoid: Monoid[Lines] = Monoid.instance(empty, _ ++ _)
}
