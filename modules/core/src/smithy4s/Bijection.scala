package smithy4s

/**
  * A bijection is an association of two opposite functions A => B and B => A.
  *
  * A bijection MUST abide by the round-tripping property, namely, for all input A :
  *
  * bijection.from(bijection(input)) == input
  */
trait Bijection[A, B] extends Function[A, B] { outer =>
  def to(a: A): B
  def from(b: B): A

  final def apply(a: A): B = to(a)

  def swap: Bijection[B, A] = Bijection(from, to)

  final def imapFull[A0, B0](
      sourceBijection: Bijection[A, A0],
      targetBijection: Bijection[B, B0]
  ): Bijection[A0, B0] = new Bijection[A0, B0] {
    def to(a0: A0): B0 = targetBijection(outer.to(sourceBijection.from(a0)))
    def from(b0: B0): A0 = sourceBijection(outer.from(targetBijection.from(b0)))
  }

  final def imapSource[A0](bijection: Bijection[A, A0]): Bijection[A0, B] =
    imapFull(bijection, Bijection.identity)

  final def imapTarget[B0](bijection: Bijection[B, B0]): Bijection[A, B0] =
    imapFull(Bijection.identity, bijection)
}

object Bijection {
  def identity[A]: Bijection[A, A] = apply(identity[A], identity[A])

  def apply[A, B](to: A => B, from: B => A): Bijection[A, B] =
    new Impl[A, B](to, from)

  private class Impl[A, B](toFunction: A => B, fromFunction: B => A)
      extends Bijection[A, B] {
    def to(a: A): B = toFunction(a)
    def from(b: B): A = fromFunction(b)
  }
}
