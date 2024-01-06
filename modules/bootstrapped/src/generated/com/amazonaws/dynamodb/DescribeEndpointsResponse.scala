package com.amazonaws.dynamodb

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct

/** @param Endpoints
  *   <p>List of endpoints.</p>
  */
final case class DescribeEndpointsResponse(endpoints: List[Endpoint])

object DescribeEndpointsResponse extends ShapeTag.Companion[DescribeEndpointsResponse] {
  val id: ShapeId = ShapeId("com.amazonaws.dynamodb", "DescribeEndpointsResponse")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[DescribeEndpointsResponse] = struct(
    Endpoints.underlyingSchema.required[DescribeEndpointsResponse]("Endpoints", _.endpoints).addHints(smithy.api.Documentation("<p>List of endpoints.</p>")),
  ){
    DescribeEndpointsResponse.apply
  }.withId(id).addHints(hints)
}
