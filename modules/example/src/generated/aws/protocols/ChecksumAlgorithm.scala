package aws.protocols

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.EnumTag
import smithy4s.schema.Schema.enumeration

/** @param CRC32C
  *   CRC32C
  * @param CRC32
  *   CRC32
  * @param SHA1
  *   SHA1
  * @param SHA256
  *   SHA256
  */
sealed abstract class ChecksumAlgorithm(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = ChecksumAlgorithm
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = ChecksumAlgorithm
  @inline final def widen: ChecksumAlgorithm = this
}
object ChecksumAlgorithm extends Enumeration[ChecksumAlgorithm] with ShapeTag.Companion[ChecksumAlgorithm] {
  val id: ShapeId = ShapeId("aws.protocols", "ChecksumAlgorithm")

  val hints: Hints = Hints(
    smithy.api.Private(),
  )

  /** CRC32C */
  case object CRC32C extends ChecksumAlgorithm("CRC32C", "CRC32C", 0, Hints(smithy.api.Documentation("CRC32C")))
  /** CRC32 */
  case object CRC32 extends ChecksumAlgorithm("CRC32", "CRC32", 1, Hints(smithy.api.Documentation("CRC32")))
  /** SHA1 */
  case object SHA1 extends ChecksumAlgorithm("SHA1", "SHA1", 2, Hints(smithy.api.Documentation("SHA1")))
  /** SHA256 */
  case object SHA256 extends ChecksumAlgorithm("SHA256", "SHA256", 3, Hints(smithy.api.Documentation("SHA256")))

  val values: List[ChecksumAlgorithm] = List(
    CRC32C,
    CRC32,
    SHA1,
    SHA256,
  )
  val tag: EnumTag = EnumTag.StringEnum
  implicit val schema: Schema[ChecksumAlgorithm] = enumeration(tag, values).withId(id).addHints(hints)
}
