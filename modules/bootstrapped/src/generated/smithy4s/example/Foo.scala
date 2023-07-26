package smithy4s.example

import smithy.api.Documentation
import smithy4s.Bijection
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
object Foo extends ShapeTag.$Companion[Foo] {

  def int(int:Int): Foo = IntCase(int)
  /** this is a comment saying you should be careful for this case
    * you never know what lies ahead with Strings like this
    */
  def str(str:String): Foo = StrCase(str)
  def bInt(bInt:BigInt): Foo = BIntCase(bInt)
  def bDec(bDec:BigDecimal): Foo = BDecCase(bDec)

  val $id: ShapeId = ShapeId("smithy4s.example", "Foo")

  val $hints: Hints = Hints(
    Documentation("Helpful information for Foo\nint, bigInt and bDec are useful number constructs\nThe string case is there because."),
  )

  final case class IntCase(int: Int) extends Foo { final def _ordinal: Int = 0 }
  /** this is a comment saying you should be careful for this case
    * you never know what lies ahead with Strings like this
    */
  final case class StrCase(str: String) extends Foo { final def _ordinal: Int = 1 }
  final case class BIntCase(bInt: BigInt) extends Foo { final def _ordinal: Int = 2 }
  final case class BDecCase(bDec: BigDecimal) extends Foo { final def _ordinal: Int = 3 }

  object IntCase {
    implicit val fromValue: Bijection[Int, IntCase] = Bijection(IntCase(_), _.int)
    implicit val toValue: Bijection[IntCase, Int] = fromValue.swap
    val $schema: Schema[IntCase] = bijection(smithy4s.schema.Schema.int, fromValue)
  }
  object StrCase {
    implicit val fromValue: Bijection[String, StrCase] = Bijection(StrCase(_), _.str)
    implicit val toValue: Bijection[StrCase, String] = fromValue.swap
    val $schema: Schema[StrCase] = bijection(string, fromValue).addHints(Documentation("this is a comment saying you should be careful for this case\nyou never know what lies ahead with Strings like this"))
  }
  object BIntCase {
    implicit val fromValue: Bijection[BigInt, BIntCase] = Bijection(BIntCase(_), _.bInt)
    implicit val toValue: Bijection[BIntCase, BigInt] = fromValue.swap
    val $schema: Schema[BIntCase] = bijection(bigint, fromValue)
  }
  object BDecCase {
    implicit val fromValue: Bijection[BigDecimal, BDecCase] = Bijection(BDecCase(_), _.bDec)
    implicit val toValue: Bijection[BDecCase, BigDecimal] = fromValue.swap
    val $schema: Schema[BDecCase] = bijection(bigdecimal, fromValue)
  }

  val int = IntCase.$schema.oneOf[Foo]("int")
  val str = StrCase.$schema.oneOf[Foo]("str")
  val bInt = BIntCase.$schema.oneOf[Foo]("bInt")
  val bDec = BDecCase.$schema.oneOf[Foo]("bDec")

  implicit val $schema: Schema[Foo] = union(
    int,
    str,
    bInt,
    bDec,
  ){
    _._ordinal
  }.withId($id).addHints($hints)
}
