package smithy4s.optics

trait Lens[S, A] {
  def get(s: S): A
  def replace(a: A): S => S
}

object Lens {

  def apply[S, A](_get: S => A)(_replace: A => S => S): Lens[S, A] =
    new Lens[S, A] {
      def get(s: S): A = _get(s)
      def replace(a: A): S => S = _replace(a)
    }

}
