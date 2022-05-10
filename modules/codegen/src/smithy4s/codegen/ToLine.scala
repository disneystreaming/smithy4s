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

import cats.implicits.toFoldableOps
import cats.kernel.Monoid
import smithy4s.codegen.LineSyntax.LineInterpolator

trait ToLine[A] {
  def render(a: A): Line
}
object ToLine {
  implicit def apply[A](implicit A: ToLine[A]): ToLine[A] = A

  implicit val identity: ToLine[Line] = r => r

  implicit val intToLine: ToLine[Int] = i => Line(i.toString)
  implicit val stringToLine: ToLine[String] = s => Line(s)
  implicit val typeToLine: ToLine[Type] = new ToLine[Type] {
    override def render(a: Type): Line = a match {
      case Type.List(member) =>
        val (imports, m) = render(member).tupled
        Line(imports, s"List[${m.mkString("")}]")
      case Type.Set(member) =>
        val (imports, m) = render(member).tupled
        Line(imports, s"Set[${m.mkString("")}]")
      case Type.Map(key, value) =>
        val (kimports, k) = render(key).tupled
        val (vimports, v) = render(value).tupled
        Line(kimports ++ vimports, s"Map[${k.mkString("")}, ${v.mkString("")}]")
      case Type.Alias(ns, name, Type.PrimitiveType(_)) =>
        Line(Set(s"$ns.$name"), name)
      case Type.Alias(ns, name, aliased) =>
        val (imports, t) = render(aliased).tupled
        Line(imports + s"$ns.$name", t)
      case Type.Ref(namespace, name) =>
        val imports = Set(s"$namespace.$name")
        Line(imports, name)
      case Type.PrimitiveType(prim) => primitiveLine(prim)
    }
  }
  private def primitiveLine(p: Primitive): Line =
    p match {
      case Primitive.Unit       => line"Unit"
      case Primitive.ByteArray  => Line(Set("schematic.ByteArray"), "ByteArray")
      case Primitive.Bool       => line"Boolean"
      case Primitive.String     => line"String"
      case Primitive.Timestamp  => line"smithy4s.Timestamp"
      case Primitive.Byte       => line"Byte"
      case Primitive.Int        => line"Int"
      case Primitive.Short      => line"Short"
      case Primitive.Long       => line"Long"
      case Primitive.Float      => line"Float"
      case Primitive.Double     => line"Double"
      case Primitive.BigDecimal => line"BigDecimal"
      case Primitive.BigInteger => line"BigInt"
      case Primitive.Uuid       => Line(Set("java.util.UUID"), "UUID")
      case Primitive.Document   => line"smithy4s.Document"
      case Primitive.Nothing    => line"Nothing"
    }
}

// Models
case class Line(imports: Set[String], line: String) {
  def tupled = (imports, line)
  def suffix(suffix: Line) = modify(s => s"$s $suffix")
  def modify(f: String => String) = Line(imports, f(line))
  def nonEmpty = line.nonEmpty
  def toLines = Lines(imports, List(line))

  def args(l: LinesWithValue*): Lines = if (l.exists(_.render.lines.nonEmpty)) {
    val openBlock = if(line.nonEmpty) line + "(" else ""
    Lines(imports, List(openBlock)) ++ indent(
      l.toList.foldMap(_.render).mapLines(_ + ",")
    ) ++ Lines(")")
  } else Lines(line + "()").addImports(imports)

}
object Line {
  def apply(line: String): Line = Line(Set.empty, line)
  def apply(importsAndLine: (Set[String], String)): Line =
    Line(importsAndLine._1, importsAndLine._2)
  val empty: Line = Line(Set.empty, "")
  implicit val monoid: Monoid[Line] = new Monoid[Line] {
    def empty = Line.empty
    def combine(a: Line, b: Line) =
      Line(a.imports ++ b.imports, a.line + b.line)
  }
}
