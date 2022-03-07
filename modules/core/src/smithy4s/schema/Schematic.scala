package smithy4s
package schema

trait Schematic[F[_]] {

  // numeric
  def short: F[Short]
  def int: F[Int]
  def float: F[Float]
  def long: F[Long]
  def double: F[Double]
  def bigint: F[BigInt]
  def bigdecimal: F[BigDecimal]

  // misc primitives
  def boolean: F[Boolean]
  def bytes: F[schematic.ByteArray]
  def uuid: F[java.util.UUID]
  def byte: F[Byte]
  def string: F[String]
  def document: F[Document]
  def unit: F[Unit]

  // collections
  def set[S](fs: F[S]): F[Set[S]]
  def list[S](fs: F[S]): F[List[S]]
  def vector[S](fs: F[S]): F[Vector[S]]
  def map[K, V](fk: F[K], fv: F[V]): F[Map[K, V]]

  // Other
  def suspend[A](f: => F[A]): F[A]
  def bijection[A, B](f: F[A], to: A => B, from: B => A): F[B]
  def withHints[A](fa: F[A], hints: smithy4s.Hints): F[A]
  def enumeration[A](
      to: A => (String, Int),
      fromName: Map[String, A],
      fromOrdinal: Map[Int, A]
  ): F[A]

  def struct[S](fields: Vector[schematic.Field[F, S, _]])(
      const: Vector[Any] => S
  ): F[S]

  def union[S](
      first: schematic.Alt[F, S, _],
      rest: Vector[schematic.Alt[F, S, _]]
  )(total: S => schematic.Alt.WithValue[F, S, _]): F[S]

}
