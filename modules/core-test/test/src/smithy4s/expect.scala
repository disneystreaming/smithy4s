/*
 *  Copyright 2021-2022 Disney Streaming
 *
 *  Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     https://disneystreaming.github.io/TOST-1.0.txt
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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
