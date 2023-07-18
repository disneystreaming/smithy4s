/*
 *  Copyright 2021-2022 Disney Streaming
 *
 *  Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     https://disneystreaming.github.io/TOST-1.0.txt
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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

    override def option[A](schema: Schema[A]): Boolean =
      self(schema)

  }

}
