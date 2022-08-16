package smithy4s.codegen

import cats.Show
import cats.implicits._
import cats.data.Chain


// LineSegment models segments of a line of code.
sealed trait LineSegment { self =>
  def toLine: Line = Line(Chain.one(self))
}
object LineSegment {
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
  case class NameDef(name: String) extends LineSegment
  object NameDef {
    implicit val nameDefShow: Show[NameDef] = Show.show[NameDef](_.name)
  }
  // A Reference to a Scala type or value s.
  case class NameRef(pkg: List[String], name: String) extends LineSegment {
    self =>
    def asValue: String = s"${(pkg :+ name).mkString(".")}"
    def asImport: String = s"${(pkg :+ name.split("\\.")(0)).mkString(".")}"
  }
  object NameRef {
    implicit val nameRefShow: Show[NameRef] = Show.show[NameRef](_.asImport)
    def apply(pkg: String, name: String): NameRef =
      NameRef(pkg.split("\\.").toList, name)
    def apply(fqn: String): NameRef = {
      val parts = fqn.split("\\.").toList
      NameRef(parts.dropRight(1), parts.last)
    }

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