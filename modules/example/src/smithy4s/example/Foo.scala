package smithy4s.example

import smithy4s.schema.syntax._

sealed trait Foo extends scala.Product with scala.Serializable
object Foo extends smithy4s.ShapeTag.Companion[Foo] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "Foo")

  val hints : smithy4s.Hints = smithy4s.Hints.empty

  case class IntCase(int: Int) extends Foo
  case class StrCase(str: String) extends Foo

  object IntCase {
    val hints : smithy4s.Hints = smithy4s.Hints.empty
    val schema: smithy4s.Schema[IntCase] = bijection(int, IntCase(_), _.int)
    val alt = schema.oneOf[Foo]("int")
  }
  object StrCase {
    val hints : smithy4s.Hints = smithy4s.Hints.empty
    val schema: smithy4s.Schema[StrCase] = bijection(string, StrCase(_), _.str)
    val alt = schema.oneOf[Foo]("str")
  }

  implicit val schema: smithy4s.Schema[Foo] = union(
    IntCase.alt,
    StrCase.alt,
  ){
    case c : IntCase => IntCase.alt(c)
    case c : StrCase => StrCase.alt(c)
  }.withId(id).addHints(hints)
}