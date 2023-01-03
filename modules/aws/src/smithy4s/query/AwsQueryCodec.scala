package smithy4s.aws.query

trait AwsQueryCodec[-A] extends (A => FormData) {
  def apply(a: A): FormData
}

object AwsQueryCodec {}
