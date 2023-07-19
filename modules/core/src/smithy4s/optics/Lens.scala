package smithy4s.optics

trait Lens[S, A] extends OpticOptional[S, A] { self =>
  def get(s: S): A
  def replace(a: A): S => S

  final def getOption(s: S): Option[A] = Some(get(s))

  override final def modify(f: A => A): S => S =
    s => replace(f(get(s)))(s)

  final def andThen[A0](that: Lens[A, A0]): Lens[S, A0] =
    new Lens[S, A0] {
      def get(s: S): A0 =
        that.get(self.get(s))
      def replace(a: A0): S => S =
        self.modify(that.replace(a))
    }

  final def some[A0](implicit
      ev1: A =:= Option[A0]
  ): OpticOptional[S, A0] =
    adapt[Option[A0]].andThen(
      Prism[Option[A0], A0](identity)(Some(_))
    )

  private[this] final def adapt[A0](implicit
      evA: A =:= A0
  ): Lens[S, A0] =
    evA.substituteCo[Lens[S, *]](this)
}

object Lens {

  def apply[S, A](_get: S => A)(_replace: A => S => S): Lens[S, A] =
    new Lens[S, A] {
      def get(s: S): A = _get(s)
      def replace(a: A): S => S = _replace(a)
    }

}
