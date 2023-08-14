package smithy4s
import smithy4s.Removable._

/**
  * ADT similar to Option, with an additional empty case representing the removal
  * of data. It is isomorphic to `Option[Option[A]]`, where the inner option
  * would represent either present or removed data.
  *
  * Its schema is encoded as an `Option[Option[A]]` schema with the addition of
  * the `alloy#nullable` trait, that helps trigger the code-generation.
  *
  * The goal of this datatype is to offer the ability to distinguish, during
  * serialisation, between the absence of a field and the nullity of a field.
  */
sealed trait Removable[+A] {
  def map[B](f: A => B): Removable[B] = this match {
    case Present(a) => Present(f(a))
    case Absent     => Absent
    case Removed    => Removed
  }

  def fold[B](whenPresent: A => B, whenAbsent: => B, whenRemoved: => B): B =
    this match {
      case Present(a) => whenPresent(a)
      case Absent     => whenAbsent
      case Removed    => whenRemoved
    }
}

object Removable {

  def present[A](a: A): Removable[A] = Present(a)
  val absent: Removable[Nothing] = Absent
  val removed: Removable[Nothing] = Removed

  case class Present[A](a: A) extends Removable[A]
  case object Absent extends Removable[Nothing]
  case object Removed extends Removable[Nothing]

  def schema[A](schemaA: Schema[A]): Schema[Removable[A]] = {
    schemaA.option.addMemberHints(alloy.Nullable()).option.biject {
      Bijection[Option[Option[A]], Removable[A]](
        {
          case Some(Some(a)) => Present(a)
          case Some(None)    => Removed
          case None          => Absent
        },
        {
          case Present(a) => Some(Some(a))
          case Removed    => Some(None)
          case Absent     => None
        }
      )
    }
  }
}
