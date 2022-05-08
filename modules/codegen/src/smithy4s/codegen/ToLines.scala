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

  implicit val stringRenderable: ToLines[String] = (s: String) =>
    Lines(Set.empty, List(s))

  //todo remove unnecessary imports at the "end of the world"
  implicit val  typeRenderable: ToLines[Type] = new ToLines[Type] {
    override def render(a: Type): Lines = a match {
      case Type.List(member) =>
        val (imports, m) = render(member).tupled
        Lines(imports, List(s"List[${m.mkString("")}]"))
      case Type.Set(member) =>
        val (imports, m) = render(member).tupled
        Lines(imports, List(s"Set[${m.mkString("")}]"))
      case Type.Map(key, value) =>
        val (kimports, k) = render(key).tupled
        val (vimports, v) = render(value).tupled
        Lines(kimports ++ vimports, List(s"Map[${k.mkString("")}, ${v.mkString("")}]"))
      case Type.Alias(ns, name, Type.PrimitiveType(_)) =>
        Lines(Set(s"$ns.$name"), List(name))
      case Type.Alias(ns, name, aliased) =>
        val (imports, t) = render(aliased).tupled
        Lines(imports + s"$ns.$name", t)
      case Type.Ref(namespace, name) =>
        val imports = Set(s"$namespace.$name")
        Lines(imports, List(name))
      case Type.PrimitiveType(prim) => renderPrimitive(prim)
    }
  }
  private def renderPrimitive(p: Primitive): Lines =
    p match {
      case Primitive.Unit => line("Unit")
      case Primitive.ByteArray =>
        Lines(Set("schematic.ByteArray"), List("ByteArray"))
      case Primitive.Bool       => line("Boolean")
      case Primitive.String     => line("String")
      case Primitive.Timestamp  => line("smithy4s.Timestamp")
      case Primitive.Byte       => line("Byte")
      case Primitive.Int        => line("Int")
      case Primitive.Short      => line("Short")
      case Primitive.Long       => line("Long")
      case Primitive.Float      => line("Float")
      case Primitive.Double     => line("Double")
      case Primitive.BigDecimal => line("BigDecimal")
      case Primitive.BigInteger => line("BigInt")
      case Primitive.Uuid => Lines(Set("java.util.UUID"), List("UUID"))
      case Primitive.Document => line("smithy4s.Document")
      case Primitive.Nothing => line("Nothing")
    }
  implicit def listRenderable[A](implicit A: ToLines[A]): ToLines[List[A]] = { (l: List[A]) =>
    val (imports, lines) = l.map(A.render).map(r => (r.imports, r.lines)).unzip
    Lines(imports.fold(Set.empty)(_ ++ _), lines.flatten)
  }

  implicit val identity: ToLines[Lines] = r => r
  implicit val renderLine: ToLines[Line] = _.toRenderResult

  implicit def tupleRenderable[A](implicit
      A: ToLines[A]
  ): ToLines[(String, A)] = (t: (String, A)) =>
    A.render(t._2).addImport(t._1)

  implicit def nelRenderable[A](implicit
      A: ToLines[A]
  ): ToLines[NonEmptyList[A]] =
    (l: NonEmptyList[A]) => listRenderable(A).render(l.toList)

  // Binding value and instance to recover from existentials
  case class WithValue[A](value: A, instance: ToLines[A]) {
    def render = instance.render(value)
  }
  object WithValue {
    // implicit conversion for use in varargs methods
    implicit def to[A](
        value: A
    )(implicit instance: ToLines[A]): ToLines.WithValue[_] =
      ToLines.WithValue(value, instance)
  }
}

trait ToLine[A] {
  def render(a: A): Line
}
object ToLine{
  implicit def apply[A](implicit A: ToLine[A]): ToLine[A] = A

}


// Models
case class Line(imports: Set[String], line: String) {
  def tupled = (imports, line)
  def suffix(suffix: Line) = modify(s => s"$s $suffix")
  def modify(f: String => String) = Line(imports, f(line))
  def nonEmpty = line.nonEmpty
  def toRenderResult = Lines(imports, List(line))

}
object Line {
  def apply(line: String): Line = Line(Set.empty, line)
  def apply(importsAndLine: (Set[String], String)): Line = Line(importsAndLine._1, importsAndLine._2)
  val empty: Line = Line(Set.empty, "")
  implicit val monoid: Monoid[Line] = new Monoid[Line] {
    def empty = Line.empty
    def combine(a: Line, b: Line) =
      Line(a.imports ++ b.imports, a.line + b.line)
  }
}


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

  def args(l: LinesWithValue*): Lines = if (l.exists(_.render.lines.nonEmpty)) {
    val openBlock = lines.lastOption.map { case line =>
      line + "("
    } match {
      case Some(value) => lines.dropRight(1).+:(value)
      case None        => lines
    }

    Lines(imports, openBlock) ++ indent(
      l.toList.foldMap(_.render).mapLines(_ + ",")
    ) ++ Lines(")")
  } else appendToLast("()")

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

  implicit val renderResultMonoid: Monoid[Lines] =
    Monoid.instance(empty, _ ++ _)
}
