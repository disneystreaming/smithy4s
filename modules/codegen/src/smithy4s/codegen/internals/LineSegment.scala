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

import cats.Show
import cats.data.Chain
import cats.implicits._

// LineSegment models segments of a line of code.
private[codegen] sealed trait LineSegment { self =>
  def toLine: Line = Line(Chain.one(self))
}
private[codegen] object LineSegment {
  // Models an Import statement, will be elided from the actual scala code
  case class Import(value: String) extends LineSegment
  object Import {
    implicit val importShow: Show[Import] = Show.show[Import](_.value)
  }
  // Models a general piece of scala code, no additional structure is known.
  case class Literal(value: String) extends LineSegment
  object Literal {
    implicit val literalShow: Show[Literal] = Show.show[Literal](_.value)
  }
  // Definition of a Scala Type like trait, class.
  case class NameDef(name: String) extends LineSegment {
    def toNameRef: NameRef = NameRef(name)
  }
  object NameDef {
    implicit val nameDefShow: Show[NameDef] = Show.show[NameDef](_.name)
  }
  // A Reference to a Scala type or value s.
  case class NameRef(pkg: List[String], name: String, typeParams: List[NameRef])
      extends LineSegment {
    self =>
    def asValue: String = s"${(pkg :+ name).mkString(".")}"

    def asImport: String = s"${(pkg :+ getNamePrefix).mkString(".")}"

    def isAutoImported: Boolean = {
      val value = pkg.mkString(".")
      value.startsWith("scala") || value.equalsIgnoreCase("java.lang")
    }
    def getNamePrefix: String = name.split("\\.").head
    def +(piece: String): NameRef = {
      self.copy(name = self.name + piece)
    }

    override def toLine: Line = {
      val paramLines =
        typeParams.map(_.toLine).foldLeft(Line.empty) { case (acc, i) =>
          if (acc.nonEmpty) acc + Literal(",") + Line.space + i else i
        }
      val selfLine = super.toLine
      if (paramLines.nonEmpty)
        selfLine + Literal("[") + paramLines + Literal("]")
      else selfLine
    }

  }

  object NameRef {
    implicit val nameRefShow: Show[NameRef] = Show.show[NameRef](_.asImport)
    def apply(pkg: String, name: String): NameRef =
      NameRef(pkg.split("\\.").toList, name, List.empty)
    def apply(fqn: String): NameRef = {
      val parts = fqn.split("\\.").toList.toNel.get
      NameRef(parts.toList.dropRight(1), parts.last, List.empty)
    }
    def apply(fqn: String, typeParams: List[NameRef]): NameRef =
      apply(fqn).copy(typeParams = typeParams)
  }

  implicit val lineSegmentShow: Show[LineSegment] = Show.show {
    case Import(value)  => value
    case Literal(value) => value
    case td: NameDef    => td.show
    case tr: NameRef    => tr.show
  }
  implicit val lineShow: Show[Line] =
    Show.show(line => line.segments.toList.map(_.show).mkString(""))

  implicit def chainShow[A: Show]: Show[Chain[A]] = Show.show { chain =>
    chain.toList.map(_.show).mkString
  }
}
