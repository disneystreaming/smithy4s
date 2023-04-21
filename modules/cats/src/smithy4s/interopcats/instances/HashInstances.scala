package smithy4s.interopcats.instances

import cats.{Eq, Hash}
import smithy4s.{ ByteArray, ShapeId, Timestamp}
import smithy4s.kinds.PolyFunction
import smithy4s.schema.Primitive

trait HashInstances {

 implicit  val byteArrayHash: Hash[ByteArray] =
    new Hash[ByteArray] {
      override def hash(x: ByteArray): Int = x.array.hashCode()

      override def eqv(x: ByteArray, y: ByteArray): Boolean =
        Eq[ByteArray].eqv(x, y)
    }
   implicit val documentHash: Hash[smithy4s.Document] = Hash.fromUniversalHashCode
   implicit val shapeIdHash: Hash[ShapeId] = Hash.fromUniversalHashCode
   implicit val timeStampHash: Hash[Timestamp] = Hash.fromUniversalHashCode
   val primHashPf: PolyFunction[Primitive, Hash] = Primitive.deriving[Hash]

}

object HashInstances extends HashInstances
