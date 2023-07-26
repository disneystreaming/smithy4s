package com.amazonaws.dynamodb

import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

final case class DescribeEndpointsRequest()
object DescribeEndpointsRequest extends ShapeTag.Companion[DescribeEndpointsRequest] {

  implicit val schema: Schema[DescribeEndpointsRequest] = constant(DescribeEndpointsRequest()).withId(ShapeId("com.amazonaws.dynamodb", "DescribeEndpointsRequest"))
  .withId(ShapeId("com.amazonaws.dynamodb", "DescribeEndpointsRequest"))
}
