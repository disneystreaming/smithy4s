package com.amazonaws.dynamodb

import smithy.api.Default
import smithy.api.Documentation
import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.long
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

/** <p>An endpoint information details.</p>
  * @param Address
  *   <p>IP address of the endpoint.</p>
  * @param CachePeriodInMinutes
  *   <p>Endpoint cache time to live (TTL) value.</p>
  */
final case class Endpoint(address: String, cachePeriodInMinutes: Long = 0L)
object Endpoint extends ShapeTag.$Companion[Endpoint] {
  val $id: ShapeId = ShapeId("com.amazonaws.dynamodb", "Endpoint")

  val $hints: Hints = Hints(
    Documentation("<p>An endpoint information details.</p>"),
  )

  val address: FieldLens[Endpoint, String] = string.required[Endpoint]("Address", _.address, n => c => c.copy(address = n)).addHints(Documentation("<p>IP address of the endpoint.</p>"), Required())
  val cachePeriodInMinutes: FieldLens[Endpoint, Long] = long.required[Endpoint]("CachePeriodInMinutes", _.cachePeriodInMinutes, n => c => c.copy(cachePeriodInMinutes = n)).addHints(Default(smithy4s.Document.fromDouble(0.0d)), Required(), Documentation("<p>Endpoint cache time to live (TTL) value.</p>"))

  implicit val $schema: Schema[Endpoint] = struct(
    address,
    cachePeriodInMinutes,
  ){
    Endpoint.apply
  }.withId($id).addHints($hints)
}
