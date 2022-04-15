package smithy4s.example

import smithy4s.syntax._

sealed trait Foo extends scala.Product with scala.Serializable
object Foo extends smithy4s.ShapeTag.Companion[Foo] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "Foo")

  val hints : smithy4s.Hints = smithy4s.Hints(
    id,
  )

  case class IntCase(int: Int) extends Foo
  case class StrCase(str: String) extends Foo
  case class BIntCase(bInt: BigInt) extends Foo
  case class BDecCase(bDec: BigDecimal) extends Foo

  object IntCase {
    val hints : smithy4s.Hints = smithy4s.Hints()
    val schema: smithy4s.Schema[IntCase] = bijection(int.withHints(hints), IntCase(_), _.int)
    val alt = schema.oneOf[Foo]("int")
  }
  object StrCase {
    val hints : smithy4s.Hints = smithy4s.Hints()
    val schema: smithy4s.Schema[StrCase] = bijection(string.withHints(hints), StrCase(_), _.str)
    val alt = schema.oneOf[Foo]("str")
  }
  object BIntCase {
    val hints : smithy4s.Hints = smithy4s.Hints()
    val schema: smithy4s.Schema[BIntCase] = bijection(bigint.withHints(hints), BIntCase(_), _.bInt)
    val alt = schema.oneOf[Foo]("bInt")
  }
  object BDecCase {
    val hints : smithy4s.Hints = smithy4s.Hints()
    val schema: smithy4s.Schema[BDecCase] = bijection(bigdecimal.withHints(hints), BDecCase(_), _.bDec)
    val alt = schema.oneOf[Foo]("bDec")
  }

  val schema: smithy4s.Schema[Foo] = union(
    IntCase.alt,
    StrCase.alt,
    BIntCase.alt,
    BDecCase.alt,
  ){
    case c : IntCase => IntCase.alt(c)
    case c : StrCase => StrCase.alt(c)
    case c : BIntCase => BIntCase.alt(c)
    case c : BDecCase => BDecCase.alt(c)
  }.withHints(hints)
  implicit val staticSchema : schematic.Static[smithy4s.Schema[Foo]] = schematic.Static(schema)
}