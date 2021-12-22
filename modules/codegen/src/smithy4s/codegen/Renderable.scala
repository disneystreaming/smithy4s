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
trait Renderable[A] {
  def render(a: A): RenderResult
}

object Renderable {

  def render[A](a: A)(implicit A: Renderable[A]): RenderResult = A.render(a)

  implicit val stringRenderable: Renderable[String] = (s: String) =>
    RenderResult(Set.empty, List(s))
  implicit def listRenderable[A](implicit
      A: Renderable[A]
  ): Renderable[List[A]] = { (l: List[A]) =>
    val (imports, lines) = l.map(A.render).map(r => (r.imports, r.lines)).unzip
    RenderResult(imports.fold(Set.empty)(_ ++ _), lines.flatten)
  }
  implicit val identity: Renderable[RenderResult] = (r: RenderResult) => r

  implicit def tupleRenderable[A](implicit
      A: Renderable[A]
  ): Renderable[(String, A)] = (t: (String, A)) =>
    A.render(t._2).addImport(t._1)

  implicit def nelRenderable[A](implicit
      A: Renderable[A]
  ): Renderable[NonEmptyList[A]] =
    (l: NonEmptyList[A]) => listRenderable(A).render(l.toList)

  // Binding value and instance to recover from existentials
  case class WithValue[A](value: A, instance: Renderable[A]) {
    def render = instance.render(value)
  }
  object WithValue {
    // implicit conversion for use in varargs methods
    implicit def to[A](
        value: A
    )(implicit instance: Renderable[A]): Renderable.WithValue[_] =
      Renderable.WithValue(value, instance)
  }
}

case class RenderResult(imports: Set[String], lines: List[String]) {
  def block(l: Lines*): RenderResult = {
    val openBlock = lines.lastOption.map {
      case ")"   => "){"
      case "}"   => "}{"
      case other => other + " {"
    } match {
      case Some(value) => lines.dropRight(1) ::: value :: Nil
      case None        => lines
    }

    RenderResult(imports, openBlock) ++ indent(
      l.toList.foldMap(_.render)
    ) ++ RenderResult("}")
  }

  def args(l: Lines*): RenderResult = if (l.exists(_.render.lines.nonEmpty)) {
    val openBlock = lines.lastOption.map { case line =>
      line + "("
    } match {
      case Some(value) => lines.dropRight(1).+:(value)
      case None        => lines
    }

    RenderResult(imports, openBlock) ++ indent(
      l.toList.foldMap(_.render).mapLines(_ + ",")
    ) ++ RenderResult(")")
  } else appendToLast("()")

  def appendToLast(s: String): RenderResult = {
    val newLines = lines.lastOption.map(_ + s) match {
      case Some(value) => lines.dropRight(1).:+(value)
      case None        => lines
    }
    RenderResult(imports, newLines)
  }

  def transformLines(f: List[String] => List[String]): RenderResult =
    RenderResult(imports, f(lines))
  def mapLines(f: String => String): RenderResult = transformLines(_.map(f))

  def ++(other: RenderResult): RenderResult =
    RenderResult(imports ++ other.imports, lines ++ other.lines)

  def addImport(im: String): RenderResult =
    if (im.nonEmpty) RenderResult(imports + im, lines) else this
  def addImports(im: Set[String]): RenderResult =
    RenderResult(imports ++ im, lines)

}
object RenderResult {
  def apply(line: String): RenderResult = RenderResult(Set.empty, List(line))
  def apply(lines: List[String]): RenderResult = RenderResult(Set.empty, lines)
  val empty = RenderResult(Set.empty, List.empty)

  implicit val renderResultMonoid: Monoid[RenderResult] =
    Monoid.instance(empty, _ ++ _)
}
