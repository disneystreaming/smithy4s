package smithy4s

import smithy4s.api.Discriminated
import smithy.api._
import cats.kernel.Eq
import smithy4s.syntax._

object HintMaskSpec extends weaver.FunSuite {

  private implicit val hintsEq: Eq[Hints] = (x: Hints, y: Hints) =>
    x.toMap == y.toMap

  test("Hints are masked") {
    val hints = Hints(Discriminated("type"), Required(), HttpError(404))
    val mask = HintMask(Discriminated.key, Required)
    val result = mask(hints)
    val expected = Hints(Discriminated("type"), Required())
    expect.eql(expected, result)
  }

  test("empty mask masks all hints") {
    val hints = Hints(Discriminated("type"), Required(), HttpError(404))
    val mask = HintMask.empty
    val result = mask(hints)
    expect.eql(Hints(), result)
  }

  type ToHints[A] = Hints

  object TestCompiler extends StubSchematic[ToHints] {
    def default[A]: Hints = Hints()

    override def withHints[A](fa: ToHints[A], hints: Hints): ToHints[A] =
      fa ++ hints
  }

  test("hint mask is applied in schematic mask") {
    val schema = string.withHints(Readonly(), Paginated())
    val mask = HintMask(Readonly)
    val newSchematic = HintMask.mask(TestCompiler, mask)
    val result = schema.compile(newSchematic)
    val expected = Hints(Readonly())
    expect.eql(
      expected,
      result
    )
  }
}
