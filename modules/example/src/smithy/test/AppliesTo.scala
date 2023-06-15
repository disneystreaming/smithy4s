package smithy.test

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.EnumTag
import smithy4s.schema.Schema.enumeration

/** @param CLIENT
  *   The test only applies to client implementations.
  * @param SERVER
  *   The test only applies to server implementations.
  */
sealed abstract class AppliesTo(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = AppliesTo
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = AppliesTo
  @inline final def widen: AppliesTo = this
}
object AppliesTo extends Enumeration[AppliesTo] with ShapeTag.Companion[AppliesTo] {
  val id: ShapeId = ShapeId("smithy.test", "AppliesTo")

  val hints: Hints = Hints(
    smithy.api.Private(),
  )

  /** The test only applies to client implementations. */
  case object CLIENT extends AppliesTo("client", "CLIENT", 0, Hints(smithy.api.Documentation("The test only applies to client implementations.")))
  /** The test only applies to server implementations. */
  case object SERVER extends AppliesTo("server", "SERVER", 1, Hints(smithy.api.Documentation("The test only applies to server implementations.")))

  val values: List[AppliesTo] = List(
    CLIENT,
    SERVER,
  )
  val tag: EnumTag = EnumTag.StringEnum
  implicit val schema: Schema[AppliesTo] = enumeration(tag, values).withId(id).addHints(hints)
}
