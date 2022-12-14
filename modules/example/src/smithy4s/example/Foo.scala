package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bigdecimal
import smithy4s.schema.Schema.bigint
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.union

sealed trait Foo extends scala.Product with scala.Serializable {
  @inline final def widen: Foo = this
}
object Foo extends ShapeTag.Companion[Foo] {
  val id: ShapeId = ShapeId("smithy4s.example", "Foo")

  val hints : Hints = Hints.empty

  case class IntCase(int: Int) extends Foo
  case class StrCase(str: String) extends Foo
  case class BIntCase(bInt: BigInt) extends Foo
  case class BDecCase(bDec: BigDecimal) extends Foo

  object IntCase {
    val hints : Hints = Hints.empty
    val schema: Schema[IntCase] = bijection(int.addHints(hints), IntCase(_), _.int)
    val alt = schema.oneOf[Foo]("int")
  }
  object StrCase {
    val hints : Hints = Hints.empty
    val schema: Schema[StrCase] = bijection(string.addHints(hints), StrCase(_), _.str)
    val alt = schema.oneOf[Foo]("str")
  }
  object BIntCase {
    val hints : Hints = Hints.empty
    val schema: Schema[BIntCase] = bijection(bigint.addHints(hints), BIntCase(_), _.bInt)
    val alt = schema.oneOf[Foo]("bInt")
  }
  object BDecCase {
    val hints : Hints = Hints.empty
    val schema: Schema[BDecCase] = bijection(bigdecimal.addHints(hints), BDecCase(_), _.bDec)
    val alt = schema.oneOf[Foo]("bDec")
  }

  implicit val schema: Schema[Foo] = union(
    IntCase.alt,
    StrCase.alt,
    BIntCase.alt,
    BDecCase.alt,
  ){
    case c : IntCase => IntCase.alt(c)
    case c : StrCase => StrCase.alt(c)
    case c : BIntCase => BIntCase.alt(c)
    case c : BDecCase => BDecCase.alt(c)
  }.withId(id).addHints(hints)
}