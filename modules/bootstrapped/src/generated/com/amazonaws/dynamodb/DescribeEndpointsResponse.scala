package com.amazonaws.dynamodb

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

/** @param Endpoints
  *   <p>List of endpoints.</p>
  */
final case class DescribeEndpointsResponse(endpoints: List[Endpoint])
object DescribeEndpointsResponse extends ShapeTag.Companion[DescribeEndpointsResponse] {
  val hints: Hints = Hints.empty

  val endpoints = Endpoints.underlyingSchema.required[DescribeEndpointsResponse]("Endpoints", _.endpoints).addHints(smithy.api.Documentation("<p>List of endpoints.</p>"), smithy.api.Required())

  implicit val schema: Schema[DescribeEndpointsResponse] = struct(
    endpoints,
  ){
    DescribeEndpointsResponse.apply
  }.withId(ShapeId("com.amazonaws.dynamodb", "DescribeEndpointsResponse")).addHints(hints)
}
