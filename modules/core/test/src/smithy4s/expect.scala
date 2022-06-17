package smithy4s

import munit.Location
import munit.Assertions
import cats.Show
import cats.Eq

object expect {

  def apply(
      cond: => Boolean,
      clue: => Any = "assertion failed"
  )(implicit loc: Location) = Assertions.assert(cond, clue)

  def same[A](left: A, right: A)(implicit
      loc: Location,
      eq: Eq[A] = Eq.fromUniversalEquals[A],
      show: Show[A] = Show.fromToString[A]
  ) =
    Assertions.assertEquals(new Wrapper(left), new Wrapper(right))

  def different[A](left: A, right: A)(implicit
      loc: Location,
      eq: Eq[A] = Eq.fromUniversalEquals[A],
      show: Show[A] = Show.fromToString[A]
  ) =
    Assertions.assertNotEquals(new Wrapper(left), new Wrapper(right))

  def eql[A](left: A, right: A)(implicit
      loc: Location,
      eq: Eq[A],
      show: Show[A] = Show.fromToString[A]
  ) =
    Assertions.assertEquals(new Wrapper(left), new Wrapper(right))

  private class Wrapper[A](val value: A)(implicit show: Show[A], eq: Eq[A]) {
    override def toString(): String = show.show(value)

    override def equals(obj: Any): Boolean =
      obj.isInstanceOf[Wrapper[_]] && eq.eqv(
        value,
        obj.asInstanceOf[Wrapper[A]].value
      )
  }

  def neql[A](left: A, right: A)(implicit
      loc: Location,
      eq: Eq[A],
      show: Show[A] = Show.fromToString[A]
  ) =
    Assertions.assertNotEquals(new Wrapper(left), new Wrapper(right))
}
