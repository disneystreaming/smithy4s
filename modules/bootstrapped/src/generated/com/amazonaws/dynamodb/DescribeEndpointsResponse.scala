package com.amazonaws.dynamodb

import smithy.api.Documentation
import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.struct

/** @param Endpoints
  *   <p>List of endpoints.</p>
  */
final case class DescribeEndpointsResponse(endpoints: List[Endpoint])
object DescribeEndpointsResponse extends ShapeTag.$Companion[DescribeEndpointsResponse] {
  val $id: ShapeId = ShapeId("com.amazonaws.dynamodb", "DescribeEndpointsResponse")

  val $hints: Hints = Hints.empty

  val endpoints: FieldLens[DescribeEndpointsResponse, List[Endpoint]] = Endpoints.underlyingSchema.required[DescribeEndpointsResponse]("Endpoints", _.endpoints, n => c => c.copy(endpoints = n)).addHints(Documentation("<p>List of endpoints.</p>"), Required())

  implicit val $schema: Schema[DescribeEndpointsResponse] = struct(
    endpoints,
  ){
    DescribeEndpointsResponse.apply
  }.withId($id).addHints($hints)
}