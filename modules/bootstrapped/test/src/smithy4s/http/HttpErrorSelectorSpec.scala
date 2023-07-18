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

import smithy4s.schema.CachedSchemaCompiler
import smithy4s.capability.Covariant
import smithy4s.example._
import ErrorHandlingServiceOperation._
import ErrorHandlingServiceExtraErrorsOperation._
final class HttpErrorSelectorSpec extends munit.FunSuite {

  type ConstId[A] = ShapeId
  implicit val covariantConstId: Covariant[ConstId] = new Covariant[ConstId] {
    def map[A, B](fa: ConstId[A])(f: A => B): ConstId[B] = fa
  }
  val compiler = new CachedSchemaCompiler[ConstId] {
    type Cache = None.type
    def createCache() = None
    def fromSchema[A](schema: Schema[A], cache: Cache): ShapeId = fromSchema(
      schema
    )
    def fromSchema[A](schema: Schema[A]): ShapeId = schema.shapeId

  }
  val selector = HttpErrorSelector(ErrorHandlingOperation.errorable, compiler)

  test("pick exact x-error-type - shapeId") {
    val result = selector(HttpDiscriminator.FullId(EHNotFound.id))
    val expected = Some(EHNotFound.id)
    assertEquals(result, expected)
  }

  test("pick exact x-error-type - name") {
    val result = selector(HttpDiscriminator.NameOnly(EHNotFound.id.name))
    val expected = Some(EHNotFound.id)

    assertEquals(result, expected)
  }

  test("pick exact x-error-type - name is case sensitive") {
    val result =
      selector(HttpDiscriminator.NameOnly(EHNotFound.id.name.toLowerCase))
    assertEquals(result, None)
  }

  test("pick exact x-error-type - status code exact match") {
    val result = selector(HttpDiscriminator.StatusCode(404))
    val expected = Some(EHNotFound.id)

    assertEquals(result, expected)
  }

  test("pick exact x-error-type - status code fallback") {
    val result = selector(HttpDiscriminator.StatusCode(400))
    val expected = Some(EHFallbackClientError.id)

    assertEquals(result, expected)
  }

  test("pick exact x-error-type - status code fallback to server-level error") {
    val result = selector(HttpDiscriminator.StatusCode(500))
    val expected = Some(EHFallbackServerError.id)

    assertEquals(result, expected)
  }

  private type GenericAlt = schema.Alt[Any, _]
  private val alts =
    ErrorHandlingOperation.error.alternatives.asInstanceOf[Vector[GenericAlt]]
  private val altsExtra =
    ExtraErrorOperation.error.alternatives
      .asInstanceOf[Vector[GenericAlt]]

  val amendedSelector = new HttpErrorSelector(alts ++ altsExtra, compiler)

  test(
    "pick exact x-error-type - status code fallback fail when multiple options - client"
  ) {
    val result = amendedSelector(HttpDiscriminator.StatusCode(460))
    assertEquals(result, None)
  }

  test(
    "pick exact x-error-type - status code fallback fail when multiple options - server"
  ) {
    val result = amendedSelector(HttpDiscriminator.StatusCode(560))
    assertEquals(result, None)
  }

  test(
    "pick exact x-error-type - status code exact fail when multiple options - client"
  ) {
    val result = amendedSelector(HttpDiscriminator.StatusCode(404))
    assertEquals(result, None)
  }

  test(
    "pick exact x-error-type - status code exact fail when multiple options - server"
  ) {
    val result = amendedSelector(HttpDiscriminator.StatusCode(503))
    assertEquals(result, None)
  }

}
