package smithy4s.interopcats.instances

import cats.Show
import smithy4s.schema.Primitive
import smithy4s.{ByteArray, Document, ShapeId, Timestamp}
import smithy4s.kinds.PolyFunction

trait ShowInstances {

  implicit  val sId: Show[ShapeId] = Show.fromToString
   implicit val byteArray: Show[ByteArray] = Show.fromToString
   implicit val document: Show[Document] = Show.fromToString
   implicit val ts: Show[Timestamp] = Show.fromToString
   val primShowPf: PolyFunction[Primitive, Show] =
    Primitive.deriving[Show]
}

object ShowInstances extends ShowInstances
