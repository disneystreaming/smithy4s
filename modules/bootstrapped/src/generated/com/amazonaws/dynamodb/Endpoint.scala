package com.amazonaws.dynamodb

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.long
import smithy4s.schema.Schema.string

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
    smithy.api.Documentation("<p>An endpoint information details.</p>"),
  )

  implicit val schema: Schema[Endpoint] = struct(
    string.required[Endpoint]("Address", _.address).addHints(smithy.api.Documentation("<p>IP address of the endpoint.</p>")),
    long.required[Endpoint]("CachePeriodInMinutes", _.cachePeriodInMinutes).addHints(smithy.api.Default(_root_.smithy4s.Document.fromDouble(0.0d)), smithy.api.Documentation("<p>Endpoint cache time to live (TTL) value.</p>")),
  ){
    Endpoint.apply
  }.withId(id).addHints(hints)
}
