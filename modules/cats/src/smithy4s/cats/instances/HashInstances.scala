package smithy4s.cats.instances

import cats.{Eq, Hash}
import cats.implicits.toContravariantOps
import smithy4s.{~>, ByteArray, ShapeId, Timestamp}
import smithy4s.schema.Primitive

trait HashInstances {

  implicit val byteArrayHash: Hash[ByteArray] =
    new Hash[ByteArray] {
      override def hash(x: ByteArray): Int = x.array.hashCode()

      override def eqv(x: ByteArray, y: ByteArray): Boolean =
        Eq[ByteArray].eqv(x, y)
    }
  implicit val documentHash: Hash[smithy4s.Document] =
    Hash[String].contramap(_.toString)
  implicit val shapeIdHash: Hash[ShapeId] = Hash[String].contramap(_.toString)
  implicit val timeStampHash: Hash[Timestamp] =
    Hash[Long].contramap(_.epochSecond)
  implicit val primHashPf: ~>[Primitive, Hash] = Primitive.deriving[Hash]

}

object HashInstances extends HashInstances
