package com.amazonaws.dynamodb

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.struct

/** @param Endpoints
  *   <p>List of endpoints.</p>
  */
final case class DescribeEndpointsResponse(endpoints: List[Endpoint])
object DescribeEndpointsResponse extends ShapeTag.Companion[DescribeEndpointsResponse] {
  val id: ShapeId = ShapeId("com.amazonaws.dynamodb", "DescribeEndpointsResponse")

  val hints: Hints = Hints.empty

  object Lenses {
    val endpoints = Lens[DescribeEndpointsResponse, List[Endpoint]](_.endpoints)(n => a => a.copy(endpoints = n))
  }

  implicit val schema: Schema[DescribeEndpointsResponse] = struct(
    Endpoints.underlyingSchema.required[DescribeEndpointsResponse]("Endpoints", _.endpoints).addHints(smithy.api.Documentation("<p>List of endpoints.</p>"), smithy.api.Required()),
  ){
    DescribeEndpointsResponse.apply
  }.withId(id).addHints(hints)
}
