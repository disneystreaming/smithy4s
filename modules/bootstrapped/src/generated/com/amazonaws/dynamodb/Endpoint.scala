package com.amazonaws.dynamodb

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.long
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

/** <p>An endpoint information details.</p>
  * 
  * @param Address
  *   <p>IP address of the endpoint.</p>
  * @param CachePeriodInMinutes
  *   <p>Endpoint cache time to live (TTL) value.</p>
  */
final case class Endpoint(address: String, cachePeriodInMinutes: Long = 0L)

object Endpoint extends ShapeTag.Companion[Endpoint] {
  val id: ShapeId = ShapeId("com.amazonaws.dynamodb", "Endpoint")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("smithy.api", "documentation"), smithy4s.Document.fromString("<p>An endpoint information details.</p>")),
  )

  // constructor using the original order from the spec
  private def make(address: String, cachePeriodInMinutes: Long): Endpoint = Endpoint(address, cachePeriodInMinutes)

  implicit val schema: Schema[Endpoint] = struct[Endpoint](
    string.required[Endpoint]("Address", _.address).addHints(Hints.dynamic(ShapeId("smithy.api", "documentation"), smithy4s.Document.fromString("<p>IP address of the endpoint.</p>"))),
    long.required[Endpoint]("CachePeriodInMinutes", _.cachePeriodInMinutes).addHints(Hints.dynamic(ShapeId("smithy.api", "default"), smithy4s.Document.fromLong(0L)), Hints.dynamic(ShapeId("smithy.api", "documentation"), smithy4s.Document.fromString("<p>Endpoint cache time to live (TTL) value.</p>"))),
  )(make).withId(id).addHints(hints)
}
