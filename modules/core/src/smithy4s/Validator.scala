package smithy4s

import smithy.api.Length
import smithy.api.Pattern
import smithy4s.capability.Isomorphism

/**
   * Given a constraint of type C, an Validator can produce a Refinement that
   * allows to go from A to B.
   *
   * A Validator can be used as a typeclass.
   */
trait Validator[C, A, B] { self =>
  def tag: ShapeTag[C]
  def make(c: C): Refinement[A, B] { type Constraint = C }
  def contramap[A0](f: A0 => A): Validator[C, A0, B] =
    new Validator[C, A0, B] {
      def tag = self.tag
      def make(c: C): Refinement[A0, B] { type Constraint = C } =
        self.make(c).contramap(f)
    }

  def map[B0](f: B => B0): Validator[C, A, B0] =
    new Validator[C, A, B0] {
      def tag = self.tag
      def make(c: C): Refinement[A, B0] { type Constraint = C } =
        self.make(c).map(f)
    }
}

object Validator {

  private abstract class SimpleImpl[C, A](implicit _tag: ShapeTag[C])
      extends Validator[C, A, A] {

    val tag: ShapeTag[C] = _tag

    def get(c: C): A => Either[String, Unit]
    final def make(c: C): Refinement.Aux[C, A, A] = new Refinement[A, A] {
      type Constraint = C
      final val tag: ShapeTag[C] = _tag
      final val constraint: C = c
      final val run = get(c)
      final def apply(a: A): Either[String, A] = run(a).map(_ => a)
    }

  }

  type Simple[C, A] = Validator[C, A, A]

  implicit def isomorphismConstraint[C, A, A0](implicit
      constraintOnA: Simple[C, A],
      iso: Isomorphism[A, A0]
  ): Simple[C, A0] = constraintOnA.contramap(iso.from).map(iso.to)

  implicit val stringLengthConstraint: Simple[Length, String] =
    new LengthConstraint[String](_.length)

  implicit val byteArrayLengthConstraint: Simple[Length, ByteArray] =
    new LengthConstraint[ByteArray](_.array.length)

  implicit def iterableLengthConstraint[C[_], A](implicit
      ev: C[A] <:< Iterable[A]
  ): Simple[Length, C[A]] =
    new LengthConstraint[C[A]](ca => ev(ca).size)

  implicit def mapLengthConstraint[K, V]: Simple[Length, Map[K, V]] =
    new LengthConstraint[Map[K, V]](_.size)

  private class LengthConstraint[A](getLength: A => Int)
      extends SimpleImpl[Length, A] {

    def get(lengthHint: Length): A => Either[String, Unit] = { (a: A) =>
      val length = getLength(a)
      (lengthHint.min, lengthHint.max) match {
        case (Some(min), Some(max)) =>
          if (length >= min && length <= max) Right(())
          else
            Left(
              s"length required to be >= $min and <= $max, but was $length"
            )
        case (Some(min), None) =>
          if (length >= min) Right(())
          else
            Left(
              s"length required to be >= $min, but was $length"
            )
        case (None, Some(max)) =>
          if (length <= max) Right(())
          else
            Left(
              s"length required to be <= $max, but was $length"
            )
        case (None, None) => Right(())
      }
    }
  }

  implicit val stringPatternConstraints: Simple[Pattern, String] =
    new SimpleImpl[Pattern, String] {

      def get(
          pattern: Pattern
      ): String => Either[String, Unit] = {
        val regex = pattern.value.r
        (input: String) =>
          if (regex.findFirstIn(input).isDefined) Right(())
          else
            Left(
              s"String '$input' does not match pattern '${pattern.value}'"
            )
      }

    }

  implicit def numericRangeConstraints[N: Numeric]
      : Simple[smithy.api.Range, N] = new SimpleImpl[smithy.api.Range, N] {

    def get(
        range: smithy.api.Range
    ): N => Either[String, Unit] = {
      val N = implicitly[Numeric[N]]

      (n: N) =>
        val value = BigDecimal(N.toDouble(n))
        (range.min, range.max) match {
          case (Some(min), Some(max)) =>
            if (value >= min && value <= max) Right(())
            else
              Left(
                s"Input must be >= $min and <= $max, but was $value"
              )
          case (None, Some(max)) =>
            if (value <= max) Right(())
            else
              Left(
                s"Input must be <= $max, but was $value"
              )
          case (Some(min), None) =>
            if (value >= min) Right(())
            else
              Left(
                s"Input must be >= $min, but was $value"
              )
          case (None, None) => Right(())
        }
    }
  }
}
