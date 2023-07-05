package smithy4s.capability
package instances

object either {

  implicit def zipperInstanceForEither[E]: Zipper[Either[E, *]] =
    new Zipper[Either[E, *]] {
      def pure[A](a: A): Either[E, A] = Right(a)

      override def zipMap[A, B, C](fa: Either[E, A], fb: Either[E, B])(
          f: (A, B) => C
      ): Either[E, C] = (fa, fb) match {
        case (l @ Left(_), _)     => l.asInstanceOf[Either[E, C]]
        case (_, r @ Left(_))     => r.asInstanceOf[Either[E, C]]
        case (Right(a), Right(b)) => Right(f(a, b))
      }

      override def zipMapAll[A](
          seq: IndexedSeq[Either[E, Any]]
      )(f: IndexedSeq[Any] => A): Either[E, A] = {
        val builder = IndexedSeq.newBuilder[Any]
        var i = 0
        var error: Left[E, Any] = null
        while (error == null && i < seq.size) {
          seq(i) match {
            case l @ Left(_) => error = l.asInstanceOf[Left[E, Any]]
            case Right(r)    => builder += r
          }
          i += 1
        }
        if (error != null) error.asInstanceOf[Left[E, A]]
        else Right(f(builder.result()))
      }
    }

}
