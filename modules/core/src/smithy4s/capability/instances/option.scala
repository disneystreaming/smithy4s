package smithy4s.capability
package instances

object option {

  implicit val optionZipper: Zipper[Option] =
    new Zipper[Option] {
      def pure[A](a: A): Option[A] = Some(a)

      override def zipMap[A, B, C](fa: Option[A], fb: Option[B])(
          f: (A, B) => C
      ): Option[C] = (fa, fb) match {
        case (None, _)          => None
        case (_, None)          => None
        case (Some(a), Some(b)) => Some(f(a, b))
      }

      override def zipMapAll[A](
          seq: IndexedSeq[Option[Any]]
      )(f: IndexedSeq[Any] => A): Option[A] = {
        val builder = IndexedSeq.newBuilder[Any]
        var i = 0
        var error: Boolean = false
        while (!error && i < seq.size) {
          seq(i) match {
            case None    => error = true
            case Some(r) => builder += r
          }
          i += 1
        }
        if (error) None
        else Some(f(builder.result()))
      }
    }

}
