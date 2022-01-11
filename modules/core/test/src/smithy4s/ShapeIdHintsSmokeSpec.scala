package smithy4s

import java.util.UUID
import schematic.ByteArray
import schematic.Field
import schematic.Alt
import cats.Id
import schematic.struct.GenericAritySchematic
import smithy4s.capability.Invariant

object ShapeIdHintsSmokeSpec extends weaver.FunSuite {

  type HintTracker[A] = TestHinted[Id, A]

  case class TestHinted[F[_], A](hints: List[Hint], make: List[Hint] => F[A]) {
    def addHints(h: List[Hint]): TestHinted[F, A] = copy(hints = hints ++ h)
    def imap[B](to: A => B, from: B => A)(implicit
        I: Invariant[F]
    ): TestHinted[F, B] =
      TestHinted(hints, h => I.imap(make(h))(to, from))
  }

  object TestHinted {
    def apply[F[_]]: PartiallyAppliedHintedTest[F] =
      new PartiallyAppliedHintedTest[F]

    class PartiallyAppliedHintedTest[F[_]] {
      def from[A](f: List[Hint] => F[A]): TestHinted[F, A] =
        TestHinted(List.empty, f(_))
    }
  }

  implicit def invariantFromCats[F[_]: cats.Invariant]: Invariant[F] =
    new Invariant[F] {
      def imap[A, B](fa: F[A])(to: A => B, from: B => A): F[B] =
        cats.Invariant[F].imap(fa)(to)(from)
    }

  private object TestCompiler
      extends Schematic[HintTracker]
      with GenericAritySchematic[HintTracker] {
    def short: HintTracker[Short] = ???

    def int: HintTracker[Int] = ???

    def long: HintTracker[Long] = ???

    def double: HintTracker[Double] = ???

    def float: HintTracker[Float] = TestHinted[Id].from(_ => 0f)

    def bigint: HintTracker[BigInt] = ???

    def bigdecimal: HintTracker[BigDecimal] = ???

    def string: HintTracker[String] = TestHinted[Id].from(_ => "")

    def boolean: HintTracker[Boolean] = ???

    def uuid: HintTracker[UUID] = ???

    def byte: HintTracker[Byte] = ???

    def bytes: HintTracker[ByteArray] = ???

    def unit: HintTracker[Unit] = ???

    def list[S](fs: HintTracker[S]): HintTracker[List[S]] = ???

    def set[S](fs: HintTracker[S]): HintTracker[Set[S]] = ???

    def vector[S](fs: HintTracker[S]): HintTracker[Vector[S]] = ???

    def map[K, V](
        fk: HintTracker[K],
        fv: HintTracker[V]
    ): HintTracker[Map[K, V]] = ???

    def genericStruct[S](fields: Vector[Field[HintTracker, S, _]])(
        const: Vector[Any] => S
    ): HintTracker[S] = {
      val result = TestHinted[Id].from(_ => const(fields))
      val allHints = fields
        .flatMap(_.instance.hints)
      result.addHints(allHints.toList)
    }

    def union[S](
        first: Alt[HintTracker, S, _],
        rest: Vector[Alt[HintTracker, S, _]]
    )(total: S => Alt.WithValue[HintTracker, S, _]): HintTracker[S] = ???

    def enumeration[A](
        to: A => (String, Int),
        fromName: Map[String, A],
        fromOrdinal: Map[Int, A]
    ): HintTracker[A] = ???

    def suspend[A](f: => HintTracker[A]): HintTracker[A] = ???

    def bijection[A, B](
        f: HintTracker[A],
        to: A => B,
        from: B => A
    ): HintTracker[B] = f.imap(to, from)

    def timestamp: HintTracker[Timestamp] = ???

    def withHints[A](fa: HintTracker[A], hints: Hints): HintTracker[A] =
      fa.addHints(hints.all.toList)

    def document: HintTracker[Document] = ???

  }

  test("newtypes contain ShapeId in hints") {
    val hintTracker = example.CityId.schema.compile(TestCompiler)
    expect(
      hintTracker.hints.contains(
        Hints.Binding.fromValue(
          ShapeId(
            "smithy4s.example",
            "CityId"
          )
        )
      )
    )
  }

  test("structure members contain ShapeId in hints") {
    val hintTracker = example.CityCoordinates.schema.compile(TestCompiler)
    expect(
      List(
        Hints.Binding.fromValue(ShapeId("smithy.api", "Float")),
        Hints.Binding.fromValue(ShapeId("smithy4s.example", "CityCoordinates"))
      ).toSet.subsetOf(hintTracker.hints.toSet)
    )
  }

  test("union members contain ShapeId in hints") {
    val hintTracker = example.ForecastResult.schema.compile(TestCompiler)
    expect(
      List(
        Hints.Binding.fromValue(ShapeId("smithy.api", "Float")),
        Hints.Binding.fromValue(ShapeId("smithy4s.example", "ForecastResult")),
        Hints.Binding.fromValue(ShapeId("smithy4s.example", "ChanceOfRain")),
        Hints.Binding.fromValue(ShapeId("smithy4s.example", "UVIndex"))
      ).toSet.subsetOf(hintTracker.hints.toSet)
    )
  }

}
