package smithy4s
package schema

private[schema] object IsPrimitive {

  private[schema] def apply[A, P](
      schema: Schema[A],
      primitive: Primitive[P]
  ): Boolean =
    schema.compile(new IsPrimitiveSchemaVisitor(primitive))

  private type BooleanConst[A] = Boolean

  private class IsPrimitiveSchemaVisitor[P](primitive: Primitive[P])
      extends smithy4s.schema.SchemaVisitor.Default[BooleanConst] { self =>

    def default[A]: Boolean = false

    override def primitive[PP](
        shapeId: ShapeId,
        hints: Hints,
        tag: Primitive[PP]
    ): Boolean = tag == primitive

    override def biject[A, B](
        schema: Schema[A],
        bijection: Bijection[A, B]
    ): Boolean = self(schema)

    override def refine[A, B](
        schema: Schema[A],
        refinement: Refinement[A, B]
    ): Boolean = self(schema)

    override def nullable[A](schema: Schema[A]): Boolean =
      self(schema)

  }

}
