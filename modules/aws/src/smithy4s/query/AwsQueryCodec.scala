package smithy4s.aws.query

private[aws] trait AwsQueryCodec[-A] extends (A => FormData) {
  def apply(a: A): FormData
}
