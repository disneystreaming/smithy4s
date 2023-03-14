package smithy4s
package aws.internals

import fs2._

private[aws] sealed trait AwsHttpBody[+F[_]] extends Product with Serializable {
  def maybeBytes: Option[Array[Byte]]
}

private[aws] object AwsHttpBody {

  // format: off
  case class Streamed[F[_]](contentLength: Long, byteStream : Stream[F, Byte]) extends AwsHttpBody[F]{
    def maybeBytes: Option[Array[Byte]] = None
  }
  case class InMemory(bytes: Array[Byte]) extends AwsHttpBody[Nothing] {
    def maybeBytes: Option[Array[Byte]] = Some(bytes)
  }
  case object Empty extends AwsHttpBody[Nothing] {
    def maybeBytes: Option[Array[Byte]] = None
  }
  // format: on
}
