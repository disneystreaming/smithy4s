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

import Schema._

// format: off
trait SchemaVisitor[F[_]] extends SchemaCompiler[Schema, F] { self =>
  final def apply[A](schema: Schema[A]) : F[A] = schema match {
    case PrimitiveSchema(shapeId, hints, tag) => primitive(shapeId, hints, tag)
    case s: CollectionSchema[c, a] => collection[c,a](s.shapeId, s.hints, s.tag, s.member)
    case MapSchema(shapeId, hints, key, value) => map(shapeId, hints, key, value)
    case EnumerationSchema(shapeId, hints, values, total) => enumeration(shapeId, hints, values, total)
    case StructSchema(shapeId, hints, fields, make) => struct(shapeId, hints, fields, make)
    case UnionSchema(shapeId, hints, alts, dispatch) => union(shapeId, hints, alts, Alt.Dispatcher(alts, dispatch))
    case BijectionSchema(schema, to, from) => biject(schema, to, from)
    case SurjectionSchema(schema, to, from) => surject(schema, to, from)
    case LazySchema(make) => lazily(make)
  }

}

object SchemaVisitor {
  abstract class Default[F[_]] extends SchemaCompiler.Default[Schema, F] with SchemaVisitor[F]
}
