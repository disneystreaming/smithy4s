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

import smithy4s.example._

import ErrorHandlingServiceGen._

final class ErrorAltPickerSpec extends munit.FunSuite {

  test("pick exact x-error-type - shapeId") {
    val alts = ErrorHandlingOperation.error.alternatives
    val errorAltPicker = new ErrorAltPicker(alts)
    val discriminator = ErrorAltPicker.ErrorDiscriminator.FullId(
      ShapeId("smithy4s.example", "EHNotFound")
    )
    val result = errorAltPicker.getPreciseAlternative(discriminator)
    val expected = Some(ErrorHandlingOperationError.EHNotFoundCase.alt)

    assertEquals(result, expected)
  }

  test("pick exact x-error-type - name") {
    val alts = ErrorHandlingOperation.error.alternatives
    val errorAltPicker = new ErrorAltPicker(alts)
    val discriminator = ErrorAltPicker.ErrorDiscriminator.NameOnly("EHNotFound")
    val result = errorAltPicker.getPreciseAlternative(discriminator)
    val expected = Some(ErrorHandlingOperationError.EHNotFoundCase.alt)

    assertEquals(result, expected)
  }

  test("pick exact x-error-type - name is case sensitive") {
    val alts = ErrorHandlingOperation.error.alternatives
    val errorAltPicker = new ErrorAltPicker(alts)
    val discriminator =
      ErrorAltPicker.ErrorDiscriminator.NameOnly("EHNotFound".toLowerCase())
    val result = errorAltPicker.getPreciseAlternative(discriminator)

    assertEquals(result, None)
  }

  test("pick exact x-error-type - status code exact match") {
    val alts = ErrorHandlingOperation.error.alternatives
    val errorAltPicker = new ErrorAltPicker(alts)
    val discriminator = ErrorAltPicker.ErrorDiscriminator.StatusCode(404)
    val result = errorAltPicker.getPreciseAlternative(discriminator)
    val expected = Some(ErrorHandlingOperationError.EHNotFoundCase.alt)

    assertEquals(result, expected)
  }

  test("pick exact x-error-type - status code fallback") {
    val alts = ErrorHandlingOperation.error.alternatives
    val errorAltPicker = new ErrorAltPicker(alts)
    val discriminator = ErrorAltPicker.ErrorDiscriminator.StatusCode(400)
    val result = errorAltPicker.getPreciseAlternative(discriminator)
    val expected =
      Some(ErrorHandlingOperationError.EHFallbackClientErrorCase.alt)

    assertEquals(result, expected)
  }

  test("pick exact x-error-type - status code fallback to server-level error") {
    val alts = ErrorHandlingOperation.error.alternatives
    val errorAltPicker = new ErrorAltPicker(alts)
    val discriminator = ErrorAltPicker.ErrorDiscriminator.StatusCode(500)
    val result = errorAltPicker.getPreciseAlternative(discriminator)
    val expected =
      Some(ErrorHandlingOperationError.EHFallbackServerErrorCase.alt)

    assertEquals(result, expected)
  }

  private type GenericAlt = schema.SchemaAlt[Any, _]
  private val alts =
    ErrorHandlingOperation.error.alternatives.asInstanceOf[Vector[GenericAlt]]
  private val altsExtra =
    ErrorHandlingServiceExtraErrorsGen.ExtraErrorOperation.error.alternatives
      .asInstanceOf[Vector[GenericAlt]]

  test(
    "pick exact x-error-type - status code fallback fail when multiple options - client"
  ) {
    val errorAltPicker = new ErrorAltPicker(alts ++ altsExtra)
    val discriminator = ErrorAltPicker.ErrorDiscriminator.StatusCode(460)
    val result =
      errorAltPicker.getPreciseAlternative(discriminator)

    assertEquals(result, None)
  }

  test(
    "pick exact x-error-type - status code fallback fail when multiple options - server"
  ) {
    val errorAltPicker = new ErrorAltPicker(alts ++ altsExtra)
    val discriminator = ErrorAltPicker.ErrorDiscriminator.StatusCode(560)
    val result =
      errorAltPicker.getPreciseAlternative(discriminator)

    assertEquals(result, None)
  }

  test(
    "pick exact x-error-type - status code exact fail when multiple options - client"
  ) {
    val errorAltPicker = new ErrorAltPicker(alts ++ altsExtra)
    val discriminator = ErrorAltPicker.ErrorDiscriminator.StatusCode(404)
    val result =
      errorAltPicker.getPreciseAlternative(discriminator)

    assertEquals(result, None)
  }

  test(
    "pick exact x-error-type - status code exact fail when multiple options - server"
  ) {
    val errorAltPicker = new ErrorAltPicker(alts ++ altsExtra)
    val discriminator = ErrorAltPicker.ErrorDiscriminator.StatusCode(503)
    val result =
      errorAltPicker.getPreciseAlternative(discriminator)

    assertEquals(result, None)
  }

}
