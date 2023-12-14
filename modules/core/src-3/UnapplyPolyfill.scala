package smithy4s

// polyfill for the difference between Scala 2 and 3's unapply for case classes
object UnapplyPolyfill {
  type Result[Tupled, CC] = CC
  def Result[Tupled, CC](f: Tupled => CC, a: Tupled): CC =
    f(a)
}
