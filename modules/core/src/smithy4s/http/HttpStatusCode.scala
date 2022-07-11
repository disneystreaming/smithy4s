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
package http

/**
  * Typeclass construct allowing to retrieve the status code associated to a value.
  */
trait HttpStatusCode[A] {

  def code(a: A, default: Int): Int

}

object HttpStatusCode {

  def apply[A](implicit instance: HttpStatusCode[A]): HttpStatusCode[A] =
    instance

  def fromSchema[A](schema: Schema[A]): HttpStatusCode[A] = {
    val go = schema.compile(internals.ErrorCodeSchemaVisitor)
    new HttpStatusCode[A] {
      def code(a: A, default: Int): Int = go(a).getOrElse(default)
    }
  }

  implicit def derivedHttpStatusCodeFromStaticSchema[A](implicit
      schema: Schema[A]
  ): HttpStatusCode[A] = statusCodeCache(schema)

  private val statusCodeCache =
    new PolyFunction[Schema, HttpStatusCode] {
      def apply[A](fa: Schema[A]): HttpStatusCode[A] = fromSchema(fa)
    }.unsafeMemoise

}
