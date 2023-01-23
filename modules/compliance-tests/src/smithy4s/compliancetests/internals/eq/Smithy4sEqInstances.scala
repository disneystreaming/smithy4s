package smithy4s.compliancetests.internals.eq

import cats.implicits.{catsSyntaxEq, toContravariantOps}
import cats.kernel.Eq
import smithy4s.{ByteArray, Document, Timestamp}
import cats.kernel.instances.StaticMethods

trait Smithy4sEqInstances {
  implicit def arrayEq[A: Eq]: Eq[Array[A]] = (x: Array[A], y: Array[A]) =>
    x.zip(y).forall { case (a, b) => a === b }

  implicit def indexedSeqEq[A: Eq]: Eq[collection.IndexedSeq[A]] = (xs: collection.IndexedSeq[A], ys: collection.IndexedSeq[A]) => if (xs eq ys) true
  else StaticMethods.iteratorEq(xs.iterator, ys.iterator)

  implicit val byteArrayEq: Eq[ByteArray] = (x: ByteArray, y: ByteArray) =>
    Eq[Array[Byte]].contramap[ByteArray](_.array).eqv(x, y)
  implicit val documentEq: Eq[Document] = Eq[String].contramap(_.show)
  implicit val timeStampEq: Eq[Timestamp] =
    Eq[Long].contramap(_.epochSecond)

}
object Smithy4sEqInstances extends Smithy4sEqInstances
