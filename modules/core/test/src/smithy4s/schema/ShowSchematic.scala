package smithy4s.schema

import smithy4s.Hints
import cats.implicits._

// Simplified version of Show, meant for testing
trait SimpleShow[A] {
  def show(a: A): String
}

object ShowSchematic extends StubSchematic[SimpleShow] {

  def default[A]: SimpleShow[A] = _ => "something"

  override def int: SimpleShow[Int] = _.toString
  override def float: SimpleShow[Float] = "%.1f".format(_)

  override def bijection[A, B](
      f: SimpleShow[A],
      to: A => B,
      from: B => A
  ): SimpleShow[B] = b => f.show(from(b))

  override def struct[S](
      fields: Vector[Field[SimpleShow, S, _]]
  )(const: Vector[Any] => S): SimpleShow[S] =
    v =>
      fields
        .map {
          def handle[A](field: Field[SimpleShow, S, A]): String = {
            val fieldValue = field.instanceA(new Field.ToOptional[SimpleShow] {
              def apply[U](fa: SimpleShow[U]): SimpleShow[Option[U]] =
                _.fold("None")(v => s"Some(${fa.show(v)})")
            })

            s"${field.label}: $fieldValue"
          }

          handle(_)
        }
        .mkString(", ")

  override def union[S](
      first: Alt[SimpleShow, S, _],
      rest: Vector[Alt[SimpleShow, S, _]]
  )(
      total: S => Alt.WithValue[SimpleShow, S, _]
  ): SimpleShow[S] = { value =>
    {
      val tot = total(value)
      (first +: rest)
        .collectFirstSome {
          def handle[A](alt: Alt[SimpleShow, S, A]) = {
            tot.matchAs(alt).map { result =>
              s"union case ${result.alt.label}: ${result.alt.instance.show(result.value)}"
            }
          }

          handle(_)
        }
        .getOrElse(sys.error("impossible - incomplete union schema"))
    }
  }
  override def withHints[A](fa: SimpleShow[A], hints: Hints): SimpleShow[A] = fa

}
