package smithy4s.optics

trait Prism[S, A] extends OpticOptional[S, A] { self =>
  def getOption(s: S): Option[A]
  def project(a: A): S

  override final def modify(f: A => A): S => S =
    s => getOption(s).fold(s)(a => project(f(a)))

  def replace(a: A): S => S =
    modify(_ => a)

  final def andThen[A0](that: Prism[A, A0]): Prism[S, A0] =
    new Prism[S, A0] {
      def getOption(s: S): Option[A0] =
        self.getOption(s).flatMap(that.getOption)
      def project(a: A0): S =
        self.project(that.project(a))
    }

  final def some[A0](implicit
      ev1: A =:= Option[A0]
  ): OpticOptional[S, A0] =
    adapt[Option[A0]].andThen(
      Prism[Option[A0], A0](identity)(Some(_))
    )

  private[this] final def adapt[A0](implicit
      evA: A =:= A0
  ): Prism[S, A0] =
    evA.substituteCo[Prism[S, *]](this)
}

object Prism {
  def apply[S, A](_get: S => Option[A])(_project: A => S): Prism[S, A] =
    new Prism[S, A] {
      def getOption(s: S): Option[A] = _get(s)
      def project(a: A): S = _project(a)
    }

  def partial[S, A](get: PartialFunction[S, A])(project: A => S): Prism[S, A] =
    Prism[S, A](get.lift)(project)
}
