package smithy4s.optics

trait Prism[S, A] {
  def get(s: S): Option[A]
  def project(a: A): S
}

object Prism {
  def apply[S, A](_get: S => Option[A])(_project: A => S): Prism[S, A] =
    new Prism[S, A] {
      def get(s: S): Option[A] = _get(s)
      def project(a: A): S = _project(a)
    }

  def partial[S, A](get: PartialFunction[S, A])(project: A => S): Prism[S, A] =
    Prism[S, A](get.lift)(project)
}
