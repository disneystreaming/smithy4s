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

package smithy4s
package http

import smithy4s.schema._
import smithy4s.http.internals.HttpResponseCodeSchemaVisitor
import smithy4s.http.internals.HttpResponseCodeSchemaVisitor.NoResponseCode
import smithy4s.http.internals.HttpResponseCodeSchemaVisitor.OptionalResponseCode
import smithy4s.http.internals.HttpResponseCodeSchemaVisitor.RequiredResponseCode

/**
  * Typeclass construct allowing to retrieve the status code associated to a value.
  */
trait HttpStatusCode[A] {

  def code(a: A, default: Int): Int

}

object HttpStatusCode extends CachedSchemaCompiler.Impl[HttpStatusCode] {

  def apply[A](implicit instance: HttpStatusCode[A]): HttpStatusCode[A] =
    instance
  type Aux[A] = internals.HttpCode[A]

  def fromSchema[A](schema: Schema[A], cache: Cache): HttpStatusCode[A] = {
    val dynamicErrorCode =
      schema.compile(new HttpResponseCodeSchemaVisitor())
    val staticErrorCode =
      schema.compile(new internals.ErrorCodeSchemaVisitor(cache))

    dynamicErrorCode match {
      case NoResponseCode =>
        (a: A, default: Int) => staticErrorCode(a).getOrElse(default)
      case OptionalResponseCode(getDynamicCode) =>
        (a: A, default: Int) =>
          getDynamicCode(a).orElse(staticErrorCode(a)).getOrElse(default)
      case RequiredResponseCode(getDynamicCode) =>
        (a: A, default: Int) => getDynamicCode(a)
    }
  }

}
