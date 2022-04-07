package smithy4s.capability

trait Isomorphism[A, B] {
  def to(a: A): B
  def from(b: B): A
}
