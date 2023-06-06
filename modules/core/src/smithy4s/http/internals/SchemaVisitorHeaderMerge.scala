package smithy4s
package http.internals

import smithy4s.schema._
import smithy4s.{Hints, ShapeId}
import smithy4s.Bijection
import smithy4s.Refinement
import smithy4s.schema.Primitive.PString
import smithy4s.schema.EnumTag.StringEnum

/**
  * A schema visitor that allows to merge several values into a single, comma-separated header value.
  * The logic for quoting is meant to abide by AWS' convoluted standards.
  *
  * See https://github.com/awslabs/smithy/pull/1798
  */
object SchemaVisitorHeaderMerge
    extends SchemaVisitor.Default[AwsMergeableHeader] {
  self =>

  def default[A]: AwsMergeableHeader[A] = None

  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): AwsMergeableHeader[P] = tag match {
    case PString =>
      Some { (str: String) =>
        if (str.contains('"') || str.contains(","))
          "\"" + str.replace("\"", "\\\"") + "\""
        else str
      }
    case _ => Primitive.stringWriter(tag, hints)
  }
  override def biject[A, B](
      schema: Schema[A],
      bijection: Bijection[A, B]
  ): AwsMergeableHeader[B] =
    schema.compile(self).map(_.compose(bijection.from(_)))

  override def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ): AwsMergeableHeader[B] =
    schema.compile(self).map(_.compose(refinement.from(_)))

  override def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      tag: EnumTag,
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): AwsMergeableHeader[E] = tag match {
    case EnumTag.IntEnum => Some((e: E) => total(e).intValue.toString())
    case StringEnum      => Some((e: E) => total(e).stringValue)
  }

}
