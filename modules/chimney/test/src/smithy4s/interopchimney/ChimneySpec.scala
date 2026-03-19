package smithy4s.interopchimney

import io.scalaland.chimney.dsl._
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ValidatedNewtype
import smithy4s.interopchimney.instances._
import weaver.FunSuite

object ChimneySpec extends FunSuite {
  object SString extends Newtype[String] {
    override def id: ShapeId = ???
    override def schema: Schema[Type] = ???
  }

  object VString extends ValidatedNewtype[String] {
    override def id: ShapeId = ???
    override def schema: Schema[Type] = ???
    override def apply(a: String): Either[String, Type] =
      Either.cond(a == "qwerty", a.asInstanceOf[VString.Type], "Not qwerty")
  }

  test("bijectionToTransformer") {
    final case class From(s: String)
    final case class To(s: SString.Type)

    expect.eql("qwerty", From("qwerty").transformInto[To].s.value)
  }

  test("bijectionFromTransformer") {
    final case class From(s: SString.Type)
    final case class To(s: String)

    expect.eql("qwerty", From(SString("qwerty")).transformInto[To].s)
  }

  test("surjectionFromTransformer") {
    final case class From(s: VString.Type)
    final case class To(s: String)

    expect.eql(
      "qwerty",
      From(VString.unsafeApply("qwerty")).transformInto[To].s
    )
  }

  test("surjectionToPartialTransformer success") {
    final case class From(s: String)
    final case class To(s: VString.Type)

    exists(From("qwerty").transformIntoPartial[To].asOption)(to =>
      expect.eql("qwerty", to.s.value)
    )
  }

  test("surjectionToPartialTransformer failure") {
    final case class From(s: String)
    final case class To(s: VString.Type)

    exists(
      From("not qwerty")
        .transformIntoPartial[To]
        .asErrorPathMessageStrings
        .headOption
    ) { case (_, error) => expect.eql("Not qwerty", error) }
  }
}
