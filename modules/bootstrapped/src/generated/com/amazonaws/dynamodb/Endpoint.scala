package com.amazonaws.dynamodb

import smithy4s.Document
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
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

object Endpoint extends ShapeTag.Companion[Endpoint] {
  val id: ShapeId = ShapeId("com.amazonaws.dynamodb", "Endpoint")

  val hints: Hints = Hints(
    ShapeId("smithy.api", "documentation") -> Document.fromString("<p>An endpoint information details.</p>"),
  )

  implicit val schema: Schema[Endpoint] = struct(
    string.required[Endpoint]("Address", _.address).addHints(ShapeId("smithy.api", "documentation") -> Document.fromString("<p>IP address of the endpoint.</p>")),
    long.required[Endpoint]("CachePeriodInMinutes", _.cachePeriodInMinutes).addHints(smithy.api.Default(smithy4s.Document.fromDouble(0.0d)), ShapeId("smithy.api", "documentation") -> Document.fromString("<p>Endpoint cache time to live (TTL) value.</p>")),
  ){
    Endpoint.apply
  }.withId(id).addHints(hints)
}
