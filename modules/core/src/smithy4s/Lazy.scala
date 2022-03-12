package smithy4s

final class Lazy[A](make: () => A) {
  private[this] var thunk: () => A = make
  lazy val value: A = {
    val result = thunk()
    thunk = null
    result
  }

  def map[B](f: A => B): Lazy[B] = new Lazy(() => f(make()))
}

object Lazy {
  def apply[A](a: => A): Lazy[A] = new Lazy(() => a)
}
