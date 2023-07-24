package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bigdecimal
import smithy4s.schema.Schema.bigint
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.union

/** Helpful information for Foo
  * int, bigInt and bDec are useful number constructs
  * The string case is there because.
  */
sealed trait Foo extends scala.Product with scala.Serializable {
  @inline final def widen: Foo = this
  def _ordinal: Int
}
object Foo extends ShapeTag.Companion[Foo] {
  val id: ShapeId = ShapeId("smithy4s.example", "Foo")

  val hints: Hints = Hints(
    smithy.api.Documentation("Helpful information for Foo\nint, bigInt and bDec are useful number constructs\nThe string case is there because."),
  )

  final case class IntCase(int: Int) extends Foo { final def _ordinal: Int = 0 }
  def int(int:Int): Foo = IntCase(int)
  /** this is a comment saying you should be careful for this case
    * you never know what lies ahead with Strings like this
    */
  final case class StrCase(str: String) extends Foo { final def _ordinal: Int = 1 }
  def str(str:String): Foo = StrCase(str)
  final case class BIntCase(bInt: BigInt) extends Foo { final def _ordinal: Int = 2 }
  def bInt(bInt:BigInt): Foo = BIntCase(bInt)
  final case class BDecCase(bDec: BigDecimal) extends Foo { final def _ordinal: Int = 3 }
  def bDec(bDec:BigDecimal): Foo = BDecCase(bDec)

  object IntCase {
    val hints: Hints = Hints.empty
    val schema: Schema[IntCase] = bijection(smithy4s.schema.Schema.int.addHints(hints), IntCase(_), _.int)
    val alt = schema.oneOf[Foo]("int")
  }
  object StrCase {
    val hints: Hints = Hints(
      smithy.api.Documentation("this is a comment saying you should be careful for this case\nyou never know what lies ahead with Strings like this"),
    )
    val schema: Schema[StrCase] = bijection(string.addHints(hints), StrCase(_), _.str)
    val alt = schema.oneOf[Foo]("str")
  }
  object BIntCase {
    val hints: Hints = Hints.empty
    val schema: Schema[BIntCase] = bijection(bigint.addHints(hints), BIntCase(_), _.bInt)
    val alt = schema.oneOf[Foo]("bInt")
  }
  object BDecCase {
    val hints: Hints = Hints.empty
    val schema: Schema[BDecCase] = bijection(bigdecimal.addHints(hints), BDecCase(_), _.bDec)
    val alt = schema.oneOf[Foo]("bDec")
  }

  implicit val schema: Schema[Foo] = union(
    IntCase.alt,
    StrCase.alt,
    BIntCase.alt,
    BDecCase.alt,
  ){
    _._ordinal
  }.withId(id).addHints(hints)
}
