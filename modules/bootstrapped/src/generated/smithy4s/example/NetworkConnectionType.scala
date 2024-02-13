package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.interopcats.SchemaVisitorHash
import smithy4s.schema.Schema.stringEnumeration

sealed abstract class NetworkConnectionType(_name: String, _stringValue: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = NetworkConnectionType
  override val name: String = _name
  override val stringValue: String = _stringValue
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = NetworkConnectionType
  @inline final def widen: NetworkConnectionType = this
}
object NetworkConnectionType extends Enumeration[NetworkConnectionType] with ShapeTag.Companion[NetworkConnectionType] {
  val id: ShapeId = ShapeId("smithy4s.example", "NetworkConnectionType")

  val hints: Hints = Hints(
    smithy4s.example.Hash(),
  ).lazily

  case object ETHERNET extends NetworkConnectionType("ETHERNET", "ETHERNET", 0, Hints.empty)
  case object WIFI extends NetworkConnectionType("WIFI", "WIFI", 1, Hints.empty)

  val values: List[NetworkConnectionType] = List(
    ETHERNET,
    WIFI,
  )
  implicit val schema: Schema[NetworkConnectionType] = stringEnumeration(values).withId(id).addHints(hints)

  implicit val networkConnectionTypeHash: cats.Hash[NetworkConnectionType] = SchemaVisitorHash.fromSchema(schema)
}
