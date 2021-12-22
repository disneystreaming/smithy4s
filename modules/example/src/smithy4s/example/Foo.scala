package smithy4s.example

import smithy4s.syntax._

sealed trait Foo
object Foo {
  def namespace: String = NAMESPACE
  val name: String = "Foo"

  val hints : smithy4s.Hints = smithy4s.Hints()

  case class IntCase(int: Int) extends Foo
  case class StrCase(str: String) extends Foo

  object IntCase {
    val hints : smithy4s.Hints = smithy4s.Hints()
    val schema: smithy4s.Schema[IntCase] = bijection(int, IntCase(_), _.int)
    val alt = schema.oneOf[Foo]("int")
  }
  object StrCase {
    val hints : smithy4s.Hints = smithy4s.Hints()
    val schema: smithy4s.Schema[StrCase] = bijection(string, StrCase(_), _.str)
    val alt = schema.oneOf[Foo]("str")
  }

  val schema: smithy4s.Schema[Foo] = union(
    IntCase.alt,
    StrCase.alt,
  ){
    case c : IntCase => IntCase.alt(c)
    case c : StrCase => StrCase.alt(c)
  }
  implicit val staticSchema : schematic.Static[smithy4s.Schema[Foo]] = schematic.Static(schema)
}