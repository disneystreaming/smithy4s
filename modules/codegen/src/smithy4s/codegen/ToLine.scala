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

import cats.data.Chain
import cats.implicits.toFoldableOps
import cats.kernel.Monoid
import smithy4s.codegen.LineSegment._

import java.util.UUID

trait ToLine[A] {
  def render(a: A): Line
}

object ToLine {
  def apply[A](implicit A: ToLine[A]): ToLine[A] = A
  implicit def lineSegmentToLine[A <: LineSegment]: ToLine[A] = l => Line(l)
  implicit val identity: ToLine[Line] = r => r
  implicit val intToLine: ToLine[Int] = i => Line(i.toString)
  implicit val bigIntToLine: ToLine[BigInt] = i => Line(i.toString)
  implicit val bigDecimalLine: ToLine[BigDecimal] = i => Line(i.toString)
  implicit val booleanToLine: ToLine[Boolean] = b =>
    Line(if (b) "true" else "false")
  implicit val doubleToLine: ToLine[Double] = d => Line(d.toString)
  implicit val floatToLine: ToLine[Float] = f => Line(f.toString)
  implicit val longToLine: ToLine[Long] = l => Line(l.toString)
  implicit val uuidToLine: ToLine[UUID] = i => Line(i.toString)

  implicit val stringToLine: ToLine[String] = s => Line(s)
  implicit val typeToLine: ToLine[Type] = new ToLine[Type] {
    override def render(a: Type): Line = a match {
      case Type.Collection(collectionType, member) =>
        val line = render(member)
        val col = collectionType.tpe
        col.toLine + Literal(
          "["
        ) + line + Literal("]")
      case Type.Map(key, value) =>
        val keyLine = render(key)
        val valueLine = render(value)
        NameRef("scala.collection.immutable", "Map").toLine + Literal(
          "["
        ) + keyLine + Literal(
          ","
        ) + valueLine + Literal("]")
      case Type.Alias(
            ns,
            name,
            Type.PrimitiveType(_) | _: Type.ExternalType,
            false
          ) =>
        NameRef(ns, name).toLine
      case Type.Alias(_, _, aliased, _) =>
        render(aliased)
      case Type.Ref(namespace, name)          => NameRef(namespace, name).toLine
      case Type.PrimitiveType(prim)           => primitiveLine(prim).toLine
      case Type.ExternalType(_, fqn, _, _, _) => NameRef(fqn).toLine
    }
  }

  private def primitiveLine(p: Primitive): NameRef = {
    def scalaP(name: String) = NameRef("scala", name)
    def javaP(name: String) = NameRef("java.lang", name)
    p match {
      case Primitive.Unit       => scalaP("Unit")
      case Primitive.ByteArray  => NameRef("smithy4s", "ByteArray")
      case Primitive.Bool       => scalaP("Boolean")
      case Primitive.String     => javaP("String")
      case Primitive.Timestamp  => NameRef("smithy4s", "Timestamp")
      case Primitive.Byte       => scalaP("Byte")
      case Primitive.Int        => scalaP("Int")
      case Primitive.Short      => scalaP("Short")
      case Primitive.Long       => scalaP("Long")
      case Primitive.Float      => scalaP("Float")
      case Primitive.Double     => scalaP("Double")
      case Primitive.BigDecimal => scalaP("BigDecimal")
      case Primitive.BigInteger => scalaP("BigInt")
      case Primitive.Uuid       => NameRef("java.util", "UUID")
      case Primitive.Document   => NameRef("smithy4s", "Document")
      case Primitive.Nothing    => NameRef("Nothing")
    }
  }
}

// Models

case class Line(segments: Chain[LineSegment]) {
  self =>
  def +(other: Line): Line = Line(self.segments ++ other.segments)

  def +(segment: LineSegment): Line = Line(self.segments :+ segment)

  def nonEmpty: Boolean = segments.nonEmpty

  /* def suffix(suffix: Line) = modify(s => s"$s $suffix")
  def addImport(imp: String) = copy(imports = imports + imp)*/

  def toLines = Lines(self)

  def args(linesWithValue: LinesWithValue*): Lines = {
    if (segments.nonEmpty) {
      if (linesWithValue.exists(_.render.list.nonEmpty))
        Lines(List(self + Literal("("))) ++ indent(
          linesWithValue.toList.foldMap(_.render).mapLines(_ + Literal(","))
        ) ++ Lines(")")
      else
        Lines(self + Literal("()"))
    } else {
      Lines.empty
    }
  }

  def appendUnlessEmpty(other: Line): Line =
    if (nonEmpty) this + other else this
}

object Line {

  def required(line: Line, default: Option[Line]): Line = {
    default match {
      case None        => line
      case Some(value) => line + Literal(" = ") + value
    }
  }

  def optional(line: Line, default: Boolean = false): Line = {
    val option =
      NameRef("scala.Option").toLine + Literal("[") + line + Literal("]")
    if (default)
      option + Literal(" = ") + NameRef("scala.None")
    else
      option
  }

  def apply(value: String): Line = Line(Chain.one(Literal(value)))

  def apply(values: LineSegment*): Line = Line(Chain(values: _*))

  val empty: Line = Line(Chain.empty)
  val comma: Line = Line(", ")
  val space: Line = Line(" ")
  val dot: Line = Line(".")
  implicit val monoid: Monoid[Line] = Monoid.instance(empty, _ + _)

}
