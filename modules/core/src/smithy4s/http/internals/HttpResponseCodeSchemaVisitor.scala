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

package smithy4s.http.internals

import smithy4s.Hints
import smithy4s.schema.SchemaField
import smithy4s.schema.SchemaVisitor
import smithy4s.ShapeId
import smithy4s.schema.Field
import smithy4s.schema.Schema

import smithy4s.http.internals.HttpResponseCodeSchemaVisitor.MaybeExtractor

class HttpResponseCodeSchemaVisitor
    extends SchemaVisitor.Default[MaybeExtractor] {
  def default[A]: MaybeExtractor[A] = None

  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[SchemaField[S, _]],
      make: IndexedSeq[Any] => S
  ): MaybeExtractor[S] = {
    def compileField[A](field: SchemaField[S, A]): MaybeExtractor[S] = {
      val folder = new Field.Folder[Schema, S, ResponseCodeExtractor[S]]() {
        def onOptional[AA](
            label: String,
            instance: Schema[AA],
            get: S => Option[AA]
        ): ResponseCodeExtractor[S] =
          new ResponseCodeExtractor[S] {
            def apply(s: S): Option[Int] = get(s).map(_.asInstanceOf[Int])
          }
        def onRequired[AA](
            label: String,
            instance: Schema[AA],
            get: S => AA
        ): ResponseCodeExtractor[S] =
          new ResponseCodeExtractor[S] {
            def apply(s: S): Option[Int] = Some(get(s).asInstanceOf[Int])
          }
      }

      field.hints
        .get[smithy.api.HttpResponseCode]
        .map(_ => field.fold(folder))
    }
    fields.flatMap(f => compileField(f)).headOption
  }
}

object HttpResponseCodeSchemaVisitor {
  type MaybeExtractor[A] = Option[ResponseCodeExtractor[A]]
}

trait ResponseCodeExtractor[A] {
  def apply(a: A): Option[Int]
}
