package smithy4s.optics

trait OpticOptional[S, A] { self =>
  def getOption(s: S): Option[A]
  def replace(a: A): S => S

  def modify(f: A => A): S => S =
    s => getOption(s).fold(s)(a => replace(f(a))(s))

  final def andThen[A0](that: OpticOptional[A, A0]): OpticOptional[S, A0] =
    new OpticOptional[S, A0] {
      def getOption(s: S): Option[A0] =
        self.getOption(s).flatMap(that.getOption)
      def replace(a: A0): S => S =
        self.modify(that.replace(a))
    }
}
