package com.amazonaws.dynamodb

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.constant

final case class DescribeEndpointsRequest()

object DescribeEndpointsRequest extends ShapeTag.Companion[DescribeEndpointsRequest] {
  val id: ShapeId = ShapeId("com.amazonaws.dynamodb", "DescribeEndpointsRequest")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[DescribeEndpointsRequest] = constant(DescribeEndpointsRequest()).withId(id).addHints(hints)
}
