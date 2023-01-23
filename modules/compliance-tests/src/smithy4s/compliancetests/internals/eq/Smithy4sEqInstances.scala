package smithy4s.compliancetests.internals.eq

import cats.implicits.{catsSyntaxEq, toContravariantOps}
import cats.kernel.Eq
import smithy4s.{ByteArray, Document, Timestamp}

trait Smithy4sEqInstances {
  implicit def arrayEq[A: Eq]: Eq[Array[A]] = (x: Array[A], y: Array[A]) =>
    x.zip(y).forall { case (a, b) => a === b }

  implicit def indexedSeq[A: Eq]: Eq[IndexedSeq[A]] = Eq[Seq[A]].contramap(_.toSeq)

  implicit val byteArrayEq: Eq[ByteArray] = (x: ByteArray, y: ByteArray) =>
    Eq[Array[Byte]].contramap[ByteArray](_.array).eqv(x, y)
  implicit val documentEq: Eq[Document] =
    implicitly[Eq[String]].contramap(_.show)
  implicit val timeStampEq: Eq[Timestamp] =
    implicitly[Eq[Long]].contramap(_.epochSecond)

}
object Smithy4sEqInstances extends Smithy4sEqInstances
