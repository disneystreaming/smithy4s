package smithy4s

import smithy4s.Schema._

case class PayloadError(
    path: PayloadPath,
    expected: String,
    message: String
) extends Throwable
    with scala.util.control.NoStackTrace {
  override def toString(): String =
    s"PayloadError($path, expected = $expected, message=$message)"
  override def getMessage(): String = s"$message (path: $path)"
}

object PayloadError {
  val schema: Schema[PayloadError] = {
    val path = PayloadPath.schema.required[PayloadError]("path", _.path)
    val expected = string.required[PayloadError]("expected", _.expected)
    val message = string.required[PayloadError]("message", _.message)
    struct(path, expected, message)(PayloadError.apply)
  }
}
