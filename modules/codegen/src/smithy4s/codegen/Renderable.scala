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
import smithy4s.codegen.test.renderResultInterpolator

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
  def typeRenderable(ns:String) :Renderable[Type] = new Renderable[Type] {
    override def render(a: Type): RenderResult = a match {
      case Type.List(member) =>
        val (imports,m) =  render(member).tupled
        RenderResult(imports,  List(s"List[${m.mkString("")}]" ))
      case Type.Set(member) =>
        val (imports,m) =  render(member).tupled
        RenderResult(imports,  List(s"Set[${m.mkString("")}]" ))
      case Type.Map(key, value) =>
        val (kimports, k) = render(key).tupled
        val (vimports, v) = render(value).tupled
        RenderResult(kimports ++ vimports,  List(s"Map[$k, $v]"))
      case Type.Alias(ns, name, Type.PrimitiveType(_)) =>
        RenderResult(Set(s"$ns.$name") , List(name))
      case Type.Alias(ns, name, aliased) =>
        val (imports, t) = render(aliased).tupled
        RenderResult(imports + s"$ns.$name",  t)
      case Type.Ref(namespace, name) =>
        val imports = if (ns != namespace) Set(s"$ns.$name") else Set.empty[String]
        RenderResult(imports , List(name))
      case Type.Alias(namespace, name, tpe) => {
       val (imports,l) =render(tpe).tupled
        RenderResult(imports ++ Set(s"$namespace.$name"), l)
      }
      case Type.PrimitiveType(prim) => renderPrimitive(prim)
    }
  }
  private def renderPrimitive(p: Primitive): RenderResult =
    p match {
      case Primitive.Unit       => line("Unit")
      case Primitive.ByteArray  => RenderResult(Set("schematic.ByteArray"), List("ByteArray"))
      case Primitive.Bool       => line("Boolean")
      case Primitive.String     => line("String")
      case Primitive.Timestamp  =>  line( "smithy4s.Timestamp")
      case Primitive.Byte       =>  line( "Byte")
      case Primitive.Int        =>  line(  "Int")
      case Primitive.Short      =>  line(  "Short")
      case Primitive.Long       =>  line(  "Long")
      case Primitive.Float      =>  line(  "Float")
      case Primitive.Double     =>  line(  "Double")
      case Primitive.BigDecimal =>  line(  "BigDecimal")
      case Primitive.BigInteger =>  line(  "BigInt")
      case Primitive.Uuid       => RenderResult(Set("java.util.UUID") ,List( "UUID"))
      case Primitive.Document   =>  line(  "smithy4s.Document")
    }
  implicit def listRenderable[A](implicit
      A: Renderable[A]
  ): Renderable[List[A]] = { (l: List[A]) =>
    val (imports, lines) = l.map(A.render).map(r => (r.imports, r.lines)).unzip
    RenderResult(imports.fold(Set.empty)(_ ++ _), lines.flatten)
  }

  implicit val identity: Renderable[RenderResult] = r => r
  implicit val renderLine:Renderable[RenderLine] = {
  case RenderLine(imports, line) => RenderResult(imports, List(line))
}
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

case class RenderLine(imports:Set[String],line:String){
  def tupled = (imports,line)
}
object RenderLine {
  implicit val renderLineRenderable: Renderable[RenderLine] = (r: RenderLine) =>
    RenderResult(r.imports, List(r.line))
  def apply(line:String): RenderLine = RenderLine(Set.empty, line)
  def apply(importsAndLine:(Set[String],String)): RenderLine = RenderLine(importsAndLine._1, importsAndLine._2)
  val empty = RenderLine(Set.empty, "")
  implicit val monoid:Monoid[RenderLine] = new Monoid[RenderLine] {
    def empty = RenderLine.empty
    def combine(a: RenderLine, b: RenderLine) = RenderLine(a.imports ++ b.imports, a.line + b.line)
  }

}
case class RenderResult(imports: Set[String], lines: List[String]) {
  def tupled = (imports,lines)
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
