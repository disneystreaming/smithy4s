package smithy4s
package schema

object syntax extends StructSyntax {

  private val prelude = "smithy.api"

  // format: off
  val short: Schema[Short] = Primitive.PShort.schema(prelude, "Short")
  val int: Schema[Int] = Primitive.PInt.schema(prelude, "Integer")
  val long: Schema[Long] = Primitive.PLong.schema(prelude, "Long")
  val double: Schema[Double] = Primitive.PDouble.schema(prelude, "Double")
  val float: Schema[Float] = Primitive.PFloat.schema(prelude, "Float")
  val bigint: Schema[BigInt] = Primitive.PBigInt.schema(prelude, "BigInteger")
  val bigdecimal: Schema[BigDecimal] = Primitive.PBigDecimal.schema(prelude, "BigDecimal")
  val string: Schema[String] = Primitive.PString.schema(prelude, "String")
  val boolean: Schema[Boolean] = Primitive.PBoolean.schema(prelude, "Boolean")
  val byte: Schema[Byte] = Primitive.PByte.schema(prelude, "Byte")
  val bytes: Schema[ByteArray] = Primitive.PBlob.schema(prelude, "Blob")
  val unit: Schema[Unit] = Primitive.PUnit.schema(prelude, "Unit")
  val timestamp: Schema[Timestamp] = Primitive.PTimestamp.schema(prelude, "Timestamp")
  val document: Schema[Document] = Primitive.PDocument.schema(prelude, "Document")
  val uuid: Schema[java.util.UUID] = Primitive.PUUID.schema("smithy4s.api", "UUID")

  override protected val placeholder: ShapeId = ShapeId("placeholder", "Placeholder")

  def list[A](a: Schema[A]): Schema[List[A]] = Schema.ListSchema(placeholder, Hints.empty, a)
  def set[A](a: Schema[A]): Schema[Set[A]] = Schema.SetSchema(placeholder, Hints.empty, a)
  def map[K, V](k: Schema[K], v: Schema[V]): Schema[Map[K, V]] = Schema.MapSchema(placeholder, Hints.empty, k, v)
  def recursive[A](s : => Schema[A]) : Schema[A] = Schema.LazySchema(Lazy(s))

  def union[U](alts: SchemaAlt[U, _]*)(dispatch: U => Alt.SchemaAndValue[U, _]) : Schema[U] =
    Schema.UnionSchema(placeholder, Hints.empty, alts.toVector, dispatch)

  def enumeration[E](total: E => EnumValue[E], values: List[EnumValue[E]]) : Schema[E] =
    Schema.EnumerationSchema(placeholder, Hints.empty, values, total)

  def bijection[A, B](a: Schema[A], to: A => B, from: B => A) : Schema[B] =
    Schema.BijectionSchema(a, to, from)

  def constant[A](a : A) : Schema[A] = Schema.StructSchema(placeholder, Hints.empty, Vector.empty, _ => a)
}
