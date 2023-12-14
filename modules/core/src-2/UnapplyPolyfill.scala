package smithy4s

// polyfill for the difference between Scala 2 and 3's unapply for case classes
// BINCOMPAT FOR 0.18 START
object UnapplyPolyfill {
  type Result[Tupled, CC] = Option[Tupled]
  def Result[Tupled, CC](f: Tupled => CC, a: Tupled): Option[Tupled] =
    Some(a)
}
// BINCOMPAT FOR 0.18 END
