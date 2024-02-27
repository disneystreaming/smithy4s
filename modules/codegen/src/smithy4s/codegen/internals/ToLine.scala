/*
 *  Copyright 2021-2024 Disney Streaming
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

import cats.data.Chain
import cats.implicits.toFoldableOps
import cats.kernel.Monoid

import java.util.UUID

import LineSegment._

private[internals] trait ToLine[A] {
  def render(a: A): Line
}

private[internals] object ToLine {
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
    override def render(a: Type): Line = typeToNameRef(a).toLine

    private def typeToNameRef(tpe: Type): NameRef = tpe match {
      case Type.Collection(collectionType, member, _) =>
        val inner = typeToNameRef(member)
        val col = collectionType.tpe
        col.copy(typeParams = List(inner))
      case Type.Map(key, _, value, _) =>
        val keyTpe = typeToNameRef(key)
        val valueTpe = typeToNameRef(value)
        NameRef("scala.collection.immutable", "Map").copy(typeParams =
          List(keyTpe, valueTpe)
        )
      case Type.Alias(
            ns,
            name,
            Type.PrimitiveType(_) | _: Type.ExternalType,
            false
          ) =>
        NameRef(ns, name)
      case Type.Alias(_, _, aliased, _) =>
        typeToNameRef(aliased)
      case Type.Ref(namespace, name) => NameRef(namespace, name)
      case Type.PrimitiveType(prim)  => primitiveLine(prim)
      case e: Type.ExternalType =>
        NameRef(e.fullyQualifiedName, e.typeParameters.map(typeToNameRef))
      case Type.Nullable(underlying) =>
        NameRef("scala", "Option").copy(typeParams =
          List(typeToNameRef(underlying))
        )
    }
  }

  private def primitiveLine(p: Primitive): NameRef = {
    def scalaP(name: String) = NameRef("scala", name)
    def javaP(name: String) = NameRef("java.lang", name)
    p match {
      case Primitive.Unit       => scalaP("Unit")
      case Primitive.Blob       => NameRef("smithy4s", "Blob")
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

private[internals] case class Line(segments: Chain[LineSegment]) {
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
          linesWithValue.toList.foldMap(_.render.mapLines(_ + Literal(",")))
        ) ++ Lines(")")
      else
        Lines(self + Literal("()"))
    } else {
      // todo: seems strange, we're discarding `linesWithValue` because `segments` is empty?
      // seems risky, perhaps it's better to throw?
      Lines.empty
    }
  }

  def appendIf(condition: this.type => Boolean)(other: Line): Line =
    if (condition(this)) this + other else this

  def when(condition: => Boolean): Line = if (condition) this else Line.empty

}

private[internals] object Line {
  import LineSyntax._

  def fieldType(field: Field) = field.modifier.typeMod match {
    case Field.TypeModification.OptionNullable =>
      Line.optional(Line.nullable(line"${field.tpe}"))
    case Field.TypeModification.Option   => Line.optional(line"${field.tpe}")
    case Field.TypeModification.Nullable => Line.nullable(line"${field.tpe}")
    case Field.TypeModification.None     => line"${field.tpe}"
  }

  private def optional(line: Line): Line = {
    NameRef("scala.Option").toLine + Literal("[") + line + Literal("]")
  }

  private def nullable(line: Line): Line = {
    NameRef("smithy4s.Nullable").toLine + Literal("[") + line + Literal("]")
  }

  def apply(value: String): Line = Line(Chain.one(Literal(value)))

  def apply(values: LineSegment*): Line = Line(Chain(values: _*))

  val empty: Line = Line(Chain.empty)
  val comma: Line = Line(", ")
  val space: Line = Line(" ")
  val dot: Line = Line(".")
  implicit val monoid: Monoid[Line] = Monoid.instance(empty, _ + _)

}
