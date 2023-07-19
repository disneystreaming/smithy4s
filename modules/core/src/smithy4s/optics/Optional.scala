package smithy4s.optics

trait Optional[S, A] { self =>
  def getOption(s: S): Option[A]
  def replace(a: A): S => S

  def modify(f: A => A): S => S =
    s => getOption(s).fold(s)(a => replace(f(a))(s))

  final def andThen[A0](that: Optional[A, A0]): Optional[S, A0] =
    new Optional[S, A0] {
      def getOption(s: S): Option[A0] =
        self.getOption(s).flatMap(that.getOption)
      def replace(a: A0): S => S =
        self.modify(that.replace(a))
    }

  def some[A0](implicit
      ev1: A =:= Option[A0]
  ): Optional[S, A0] =
    adapt[Option[A0]].andThen(
      Prism[Option[A0], A0](identity)(Some(_))
    )

  private[this] final def adapt[A0](implicit
      evA: A =:= A0
  ): Optional[S, A0] =
    evA.substituteCo[Optional[S, *]](this)
}
