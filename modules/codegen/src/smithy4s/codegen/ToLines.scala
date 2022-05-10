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

package smithy4s.codegen

import cats.data.NonEmptyList
import cats.kernel.Monoid
import cats.syntax.all._

/**
  * Construct allowing to flatten arbitrary levels of nested lists
  */
trait ToLines[A] {
  def render(a: A): Lines
}

object ToLines {

  def render[A](a: A)(implicit A: ToLines[A]): Lines = A.render(a)
  implicit val identity: ToLines[Lines] = r => r

  implicit def listToLines[A](implicit A: ToLines[A]): ToLines[List[A]] = {
    (l: List[A]) =>
      val (imports, lines) =
        l.map(A.render).map(r => (r.imports, r.lines)).unzip
      Lines(imports.fold(Set.empty)(_ ++ _), lines.flatten)
  }

  implicit def tupleRenderable[A](implicit
      A: ToLines[A]
  ): ToLines[(String, A)] = (t: (String, A)) => A.render(t._2).addImport(t._1)

  implicit def nelRenderable[A](implicit
      A: ToLines[A]
  ): ToLines[NonEmptyList[A]] =
    (l: NonEmptyList[A]) => listToLines(A).render(l.toList)

  implicit def lineToLines[A: ToLine]: ToLines[A] = (a: A) => {
    val (imports, line) = ToLine[A].render(a).tupled
    // empty string must be treated like an empty list which is a Monoid Empty as oposed to wrapping in a singleton list which will render a new line character`
    if (line.isEmpty) Lines.empty else Lines(imports, List(line))
  }

}

// Models

case class Lines(imports: Set[String], lines: List[String]) {
  def tupled = (imports, lines)
  def block(l: LinesWithValue*): Lines = {
    val openBlock = lines.lastOption.map {
      case ")"   => "){"
      case "}"   => "}{"
      case other => other + " {"
    } match {
      case Some(value) => lines.dropRight(1) ::: value :: Nil
      case None        => lines
    }

    Lines(imports, openBlock) ++ indent(
      l.toList.foldMap(_.render)
    ) ++ Lines("}")
  }

  def appendToLast(s: String): Lines = {
    val newLines = lines.lastOption.map(_ + s) match {
      case Some(value) => lines.dropRight(1).:+(value)
      case None        => lines
    }
    Lines(imports, newLines)
  }

  def transformLines(f: List[String] => List[String]): Lines =
    Lines(imports, f(lines))
  def mapLines(f: String => String): Lines = transformLines(_.map(f))

  def ++(other: Lines): Lines =
    Lines(imports ++ other.imports, lines ++ other.lines)

  def addImport(im: String): Lines =
    if (im.nonEmpty) Lines(imports + im, lines) else this
  def addImports(im: Set[String]): Lines =
    Lines(imports ++ im, lines)

}
object Lines {
  def apply(line: String): Lines = Lines(Set.empty, List(line))
  def apply(lines: List[String]): Lines = Lines(Set.empty, lines)
  val empty = Lines(Set.empty, List.empty)
  implicit val linesMonoid: Monoid[Lines] = Monoid.instance(empty, _ ++ _)
}
