package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.interopcats.SchemaVisitorHash
import smithy4s.schema.EnumTag
import smithy4s.schema.Schema.enumeration

sealed abstract class NetworkConnectionType(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = NetworkConnectionType
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = NetworkConnectionType
  @inline final def widen: NetworkConnectionType = this
}
object NetworkConnectionType extends Enumeration[NetworkConnectionType] with ShapeTag.Companion[NetworkConnectionType] {
  case object ETHERNET extends NetworkConnectionType("ETHERNET", "ETHERNET", 0, Hints())
  case object WIFI extends NetworkConnectionType("WIFI", "WIFI", 1, Hints())

  val values: List[NetworkConnectionType] = List(
    ETHERNET,
    WIFI,
  )
  val tag: EnumTag = EnumTag.StringEnum
  implicit val schema: Schema[NetworkConnectionType] = enumeration(tag, values)
  .withId(ShapeId("smithy4s.example", "NetworkConnectionType"))
  .addHints(
    smithy4s.example.Hash(),
  )

  implicit val networkConnectionTypeHash: cats.Hash[NetworkConnectionType] = SchemaVisitorHash.fromSchema(schema)
}
