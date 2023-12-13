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
sealed trait Foo extends scala.Product with scala.Serializable { self =>
  @inline final def widen: Foo = this
  def $ordinal: Int

  object project {
    def int: Option[Int] = Foo.IntCase.alt.project.lift(self).map(_.int)
    def str: Option[String] = Foo.StrCase.alt.project.lift(self).map(_.str)
    def bInt: Option[BigInt] = Foo.BIntCase.alt.project.lift(self).map(_.bInt)
    def bDec: Option[BigDecimal] = Foo.BDecCase.alt.project.lift(self).map(_.bDec)
  }

  def accept[A](visitor: Foo.Visitor[A]): A = this match {
    case value: Foo.IntCase => visitor.int(value.int)
    case value: Foo.StrCase => visitor.str(value.str)
    case value: Foo.BIntCase => visitor.bInt(value.bInt)
    case value: Foo.BDecCase => visitor.bDec(value.bDec)
  }
}
object Foo extends ShapeTag.Companion[Foo] {

  def int(int: Int): Foo = IntCase(int)
  /** this is a comment saying you should be careful for this case
    * you never know what lies ahead with Strings like this
    */
  def str(str: String): Foo = StrCase(str)
  def bInt(bInt: BigInt): Foo = BIntCase(bInt)
  def bDec(bDec: BigDecimal): Foo = BDecCase(bDec)

  val id: ShapeId = ShapeId("smithy4s.example", "Foo")

  val hints: Hints = Hints.lazily(
    Hints(
      smithy.api.Documentation("Helpful information for Foo\nint, bigInt and bDec are useful number constructs\nThe string case is there because."),
    )
  )

  final case class IntCase(int: Int) extends Foo { final def $ordinal: Int = 0 }
  /** this is a comment saying you should be careful for this case
    * you never know what lies ahead with Strings like this
    */
  final case class StrCase(str: String) extends Foo { final def $ordinal: Int = 1 }
  final case class BIntCase(bInt: BigInt) extends Foo { final def $ordinal: Int = 2 }
  final case class BDecCase(bDec: BigDecimal) extends Foo { final def $ordinal: Int = 3 }

  object IntCase {
    val hints: Hints = Hints.empty
    val schema: Schema[Foo.IntCase] = bijection(smithy4s.schema.Schema.int.addHints(hints), Foo.IntCase(_), _.int)
    val alt = schema.oneOf[Foo]("int")
  }
  object StrCase {
    val hints: Hints = Hints.lazily(
      Hints(
        smithy.api.Documentation("this is a comment saying you should be careful for this case\nyou never know what lies ahead with Strings like this"),
      )
    )
    val schema: Schema[Foo.StrCase] = bijection(string.addHints(hints), Foo.StrCase(_), _.str)
    val alt = schema.oneOf[Foo]("str")
  }
  object BIntCase {
    val hints: Hints = Hints.empty
    val schema: Schema[Foo.BIntCase] = bijection(bigint.addHints(hints), Foo.BIntCase(_), _.bInt)
    val alt = schema.oneOf[Foo]("bInt")
  }
  object BDecCase {
    val hints: Hints = Hints.empty
    val schema: Schema[Foo.BDecCase] = bijection(bigdecimal.addHints(hints), Foo.BDecCase(_), _.bDec)
    val alt = schema.oneOf[Foo]("bDec")
  }

  trait Visitor[A] {
    def int(value: Int): A
    def str(value: String): A
    def bInt(value: BigInt): A
    def bDec(value: BigDecimal): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def int(value: Int): A = default
      def str(value: String): A = default
      def bInt(value: BigInt): A = default
      def bDec(value: BigDecimal): A = default
    }
  }

  implicit val schema: Schema[Foo] = union(
    Foo.IntCase.alt,
    Foo.StrCase.alt,
    Foo.BIntCase.alt,
    Foo.BDecCase.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
