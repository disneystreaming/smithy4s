package smithy4s.http

sealed trait HttpUriScheme

object HttpUriScheme {
  case object Http extends HttpUriScheme
  case object Https extends HttpUriScheme
}
