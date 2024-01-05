/*
 *  Copyright 2021-2024 Disney Streaming
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

package smithy4s.dynamic.internals

import smithy4s.schema.ErrorSchema
import smithy4s.schema.Schema

private[internals] object DynamicErrorSchema {

  def apply[E](schema: Schema[E]): ErrorSchema[E] = {
    def liftError(throwable: Throwable): Option[E] = throwable match {
      case DynamicError(shapeId, e) if shapeId == schema.shapeId =>
        Some(e.asInstanceOf[E])
      case _ => None
    }
    def unliftError(e: E): Throwable = DynamicError(schema.shapeId, e)
    ErrorSchema(schema, liftError, unliftError)
  }

}
