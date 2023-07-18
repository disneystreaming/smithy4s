package com.amazonaws.dynamodb

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
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
    smithy.api.Documentation("<p>An endpoint information details.</p>"),
  )

  object Lenses {
    val address = Lens[Endpoint, String](_.address)(n => a => a.copy(address = n))
    val cachePeriodInMinutes = Lens[Endpoint, Long](_.cachePeriodInMinutes)(n => a => a.copy(cachePeriodInMinutes = n))
  }

  implicit val schema: Schema[Endpoint] = struct(
    string.required[Endpoint]("Address", _.address).addHints(smithy.api.Documentation("<p>IP address of the endpoint.</p>"), smithy.api.Required()),
    long.required[Endpoint]("CachePeriodInMinutes", _.cachePeriodInMinutes).addHints(smithy.api.Default(smithy4s.Document.fromDouble(0.0d)), smithy.api.Required(), smithy.api.Documentation("<p>Endpoint cache time to live (TTL) value.</p>")),
  ){
    Endpoint.apply
  }.withId(id).addHints(hints)
}
