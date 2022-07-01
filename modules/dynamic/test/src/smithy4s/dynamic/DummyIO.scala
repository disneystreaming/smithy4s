package smithy4s.dynamic

object DummyIO {
  type IO[A] = Either[Throwable, A]
  object IO {
    def apply[A](a: => A) = try { Right(a) }
    catch { case e: Throwable => Left(e) }
    def pure[A](a: A): IO[A] = Right(a)
    def raiseError[A](e: Throwable): IO[A] = Left(e)
  }
  implicit class IOOps[A](private val io: IO[A]) extends AnyVal {
    def mapRun[B](f: A => B): B = io match {
      case Left(e)  => throw e
      case Right(a) => f(a)
    }
    def check(): Unit = io match {
      case Left(e) => throw e
      case Right(_) => ()
    }
  }
}
