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

import cats.implicits.toFoldableOps
import cats.kernel.Monoid
import cats.data.Chain
import cats.Show
import cats.syntax.all._
import smithy4s.codegen.LineSegment.{Hardcoded, TypeDefinition, TypeReference}


trait ToLine[A] {
  def render(a: A): Line
}

object ToLine {
  def apply[A](implicit A: ToLine[A]): ToLine[A] = A
  implicit val lineSegment:ToLine[LineSegment] = l => Line(l)
  implicit val identity: ToLine[Line] = r => r
  implicit val intToLine: ToLine[Int] = i => Line(i.toString)
  implicit val stringToLine: ToLine[String] = s => Line(s)
  implicit val typeToLine: ToLine[Type] = new ToLine[Type] {
    override def render(a: Type): Line = a match {
      case Type.Collection(collectionType, member) =>
        val line  = render(member)
        val col = collectionType.tpe
       TypeReference(col) :+ Hardcoded("[") :++ line :+ Hardcoded("]")
      case Type.Map(key, value) =>
        val keyLine = render(key)
        val valueLine = render(value)
        TypeReference("Map") :+ Hardcoded("[") :++ Line((keyLine.segments :+ Hardcoded(",")) ++ valueLine.segments :+ Hardcoded("]"))
      case Type.Alias(ns, name, Type.PrimitiveType(_)) => TypeReference(ns, name)
      case Type.Alias(_, _, aliased) => render(aliased)
      case Type.Ref(namespace, name) => TypeReference(namespace, name)
      case Type.PrimitiveType(prim) => primitiveLine(prim)
    }
  }
  private def primitiveLine(p: Primitive): Line =
    p match {
      case Primitive.Unit       => TypeReference("Unit")
      case Primitive.ByteArray  => TypeReference("smithy4s", "ByteArray")
      case Primitive.Bool       => TypeReference("Boolean")
      case Primitive.String     => TypeReference("String")
      case Primitive.Timestamp  => TypeReference("smithy4s", "Timestamp")
      case Primitive.Byte       => TypeReference("Byte")
      case Primitive.Int        => TypeReference("Int")
      case Primitive.Short      => TypeReference("Short")
      case Primitive.Long       => TypeReference("Long")
      case Primitive.Float      => TypeReference("Float")
      case Primitive.Double     => TypeReference("Double")
      case Primitive.BigDecimal => TypeReference("BigDecimal")
      case Primitive.BigInteger => TypeReference("BigInt")
      case Primitive.Uuid       => TypeReference("java.util", "UUID")
      case Primitive.Document   => TypeReference("smithy4s", "Document")
      case Primitive.Nothing    => TypeReference("Nothing")
    }
}

// Models

sealed trait LineSegment{ self =>
  def toLine: Line = Line(Chain.one(self))
}
object LineSegment {
  case class Hardcoded(value: String) extends LineSegment
  case class TypeDefinition(pkg: Set[String], name: String)  extends LineSegment
  case class TypeReference(pkg: Set[String], name: String) extends LineSegment
  object TypeReference{
    def apply(pkg: String, name: String): Line = TypeReference(pkg.split("\\.").toSet, name).toLine
    def apply(fqn:String):Line = {
      val parts = fqn.split("\\.").toList
      TypeReference(parts.dropRight(1).toSet,parts.last).toLine
    }

  }

  implicit val hardcodedShow = Show.show[Hardcoded](hc => hc.value)
  implicit val typeDefShow:Show[TypeDefinition] = Show.show{
    td =>  s"${(td.pkg + td.name).mkString(".")}"
  }
  implicit val typeRefShow:Show[TypeReference] = Show.show{
    tr =>  s"${(tr.pkg + tr.name).mkString(".")}"
  }
  implicit val lineSegmentShow:Show[LineSegment] = Show.show {
    case Hardcoded(value) => value
    case TypeDefinition(pkg, name) =>   s"${(pkg + name).mkString(".")}"
    case TypeReference(pkg, name) =>  s"${(pkg + name).mkString(".")}"
  }
  implicit val lineShow:Show[Line] = Show.show(line => line.segments.toList.map(_.show).mkString(""))

    implicit def chainShow[A:Show]: Show[Chain[A]] = Show.show{
      chain => chain.toList.map(_.show).mkString
    }
}
case class Line(segments:Chain[LineSegment]) {
  self =>
  def :++(other: Line): Line = Line(self.segments ++ other.segments)

  def +(segment: LineSegment): Line = Line(self.segments :+ segment)

  def :+(segment: LineSegment): Line = Line(self.segments :+ segment)

  def +:(segment:LineSegment):Line = Line(segment +: self.segments)


  def nonEmpty: Boolean = segments.nonEmpty

  /* def suffix(suffix: Line) = modify(s => s"$s $suffix")
  def addImport(imp: String) = copy(imports = imports + imp)*/

  def toLines = Lines(self)

  def args(l: LinesWithValue*): Lines = {
    if (segments.nonEmpty) {
      if (l.exists(_.render.list.nonEmpty))
        Lines(List(self + Hardcoded("("))) ++ indent(l.toList.foldMap(_.render).mapLines(_ + Hardcoded(","))) ++ Lines(")")
      else
        Lines(self  + Hardcoded("()"))
    }
    else {
      Lines.empty
    }
  }
}

object Line {


  def optional(line: Line, default:Boolean=false):Line = {
   val option = TypeReference("Option") :+ Hardcoded("[") :++ line + Hardcoded("]")
    if(default)
      option :+ Hardcoded("=") :++ TypeReference("None")
    else
      option
  }

  def apply(value: String): Line = Line(Chain.one(Hardcoded(value)))

  def apply(values: LineSegment*): Line = Line(Chain(values: _*))

  def typeDefinition(pkg: String, name: String): Line = TypeDefinition(pkg.split(".").toSet, name).toLine

  def typeDefinition(name: String): Line = TypeDefinition(Set.empty, name).toLine


  val empty: Line = Line(Chain.empty)
  val comma: Line = Line(", ")
  val dot: Line = Line(".")
  implicit val monoid: Monoid[Line] = Monoid.instance(empty, _ :++ _)

}
