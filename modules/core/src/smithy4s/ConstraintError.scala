package smithy4s

final case class ConstraintError(hint: Hint, message: String)
    extends Throwable
    with scala.util.control.NoStackTrace {
  override def getMessage() = s"${hint.value}: $message"
}
