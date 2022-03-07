package smithy4s

class Lazy[A](make: () => A) {
  private[this] var thunk: () => A = make
  lazy val value: A = {
    val result = thunk()
    thunk = null
    result
  }

  def map[B](f: A => B): Lazy[B] = new Lazy(() => f(make()))
}
