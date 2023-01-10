package smithy4s.example.refined

import smithy4s._

final case class NonEmptyMap[K, V] private (values: Map[K, V])

object NonEmptyMap {

  def apply[K, V](values: Map[K, V]): Either[String, NonEmptyMap[K, V]] =
    if (values.size > 0) Right(new NonEmptyMap(values))
    else Left("Map must not be empty.")

  implicit def provider[K, V] = Refinement.drivenBy[smithy4s.example.NonEmptyMapFormat](
    NonEmptyMap.apply[K, V],
    (b: NonEmptyMap[K, V]) => b.values
  )
}
