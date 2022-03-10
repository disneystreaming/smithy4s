package smithy4s
package schema

sealed trait Primitive[T] {
  final def schema(namespace: String, name: String): Schema[T] =
    Schema.PrimitiveSchema(ShapeId(namespace, name), Hints.empty, this)
}

object Primitive {

  case object PShort extends Primitive[Short]
  case object PInt extends Primitive[Int]
  case object PFloat extends Primitive[Float]
  case object PLong extends Primitive[Long]
  case object PDouble extends Primitive[Double]
  case object PBigInt extends Primitive[BigInt]
  case object PBigDecimal extends Primitive[BigDecimal]

  case object PBoolean extends Primitive[Boolean]
  case object PString extends Primitive[String]
  case object PUUID extends Primitive[java.util.UUID]
  case object PByte extends Primitive[Byte]
  case object PBlob extends Primitive[ByteArray]
  case object PDocument extends Primitive[Document]
  case object PTimestamp extends Primitive[Timestamp]
  case object PUnit extends Primitive[Unit]

}
