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

package smithy4s.compliancetests
package internals

import cats.implicits._
import ComplianceTest._
import cats.Eq
import io.circe.Json
import org.http4s.Headers
import org.typelevel.ci.CIString
import smithy.test.{HttpRequestTestCase, HttpResponseTestCase}
import io.circe.parser._

private[internals] object assert {

  def success: ComplianceResult = ().validNel
  def fail(msg: String): ComplianceResult = msg.invalidNel[Unit]

  private def isJson(bodyMediaType: Option[String]) =
    bodyMediaType.forall(_.equalsIgnoreCase("application/json"))

  private def jsonEql(result: String, testCase: String): ComplianceResult = {
    (result.isEmpty, testCase.isEmpty) match {
      case (true, true) => success
      case _ =>
        val nonEmpty = if (result.isEmpty) "{}" else result
        (parse(result), parse(nonEmpty)) match {
          case (Right(a), Right(b)) if Eq[Json].eqv(a, b) => success
          case (Left(a), Left(b)) => fail(s"Both JSONs are invalid: $a, $b")
          case (Left(a), _) =>
            fail(s"Result JSON is invalid: $result \n Error $a ")
          case (_, Left(b)) =>
            fail(s"TestCase JSON is invalid: $testCase \n Error $b")
          case (Right(a), Right(b)) =>
            fail(s"JSONs are not equal: result json: $a \n testcase json:  $b")
        }
    }
  }

  def eql[A: Eq](
      result: A,
      testCase: A,
      prefix: String = ""
  ): ComplianceResult = {
    if (result === testCase) {
      success
    } else {
      fail(
        s"$prefix the result value: ${pprint.apply(result)} was not equal to the expected TestCase value ${pprint
          .apply(testCase)}."
      )
    }
  }

  def bodyEql(
      result: String,
      testCase: String,
      bodyMediaType: Option[String]
  ): ComplianceResult = {
    if (isJson(bodyMediaType)) {
      jsonEql(result, testCase)
    } else {
      eql(result, testCase)
    }
  }

  private def queryParamsExistenceCheck(
      queryParameters: Map[String, Seq[String]],
      requiredParameters: Option[List[String]],
      forbiddenParameters: Option[List[String]]
  ) = {
    val receivedParams = queryParameters.keySet
    val checkRequired = requiredParameters.foldMap { requiredParams =>
      requiredParams.traverse_ { param =>
        val errorMessage =
          s"Required query parameter $param was not present in the request"

        if (receivedParams.contains(param)) success
        else fail(errorMessage)
      }
    }
    val checkForbidden = forbiddenParameters.foldMap { forbiddenParams =>
      forbiddenParams.traverse_ { param =>
        val errorMessage =
          s"Forbidden query parameter $param was present in the request"
        if (receivedParams.contains(param)) fail(errorMessage)
        else success
      }
    }
    checkRequired |+| checkForbidden
  }

  private def queryParamValuesCheck(
      queryParameters: Map[String, Seq[String]],
      testCase: Option[List[String]]
  ) = {
    testCase.toList.flatten
      .map(splitQuery)
      .collect {
        case (key, _) if !queryParameters.contains(key) =>
          fail(s"missing query parameter $key")
        case (key, expectedValue)
            if !queryParameters
              .get(key)
              .toList
              .flatten
              .contains(expectedValue) =>
          fail(
            s"query parameter $key has value ${queryParameters.get(key).toList.flatten} but expected $expectedValue"
          )
        case _ => success
      }
      .combineAll
  }

  private def headersExistenceCheck(
      headers: Headers,
      requiredHeaders: Option[List[String]],
      forbiddenHeaders: Option[List[String]]
  ) = {
    val checkRequired = requiredHeaders.toList.flatten.collect {
      case key if headers.get(CIString(key)).isEmpty =>
        assert.fail(s"Header $key is required request.")
    }.combineAll
    val checkForbidden = forbiddenHeaders.toList.flatten.collect {
      case key if headers.get(CIString(key)).isDefined =>
        assert.fail(s"Header $key is forbidden in the request.")
    }.combineAll
    checkRequired |+| checkForbidden
  }
  private def headerValuesCheck(
      headers: Headers,
      expected: Option[Map[String, String]]
  ) = {
    expected.toList
      .flatMap(_.toList)
      .map { case (key, expectedValue) =>
        headers
          .get(CIString(key))
          .map { v =>
            assert.eql[String](v.head.value, expectedValue, s"Header $key: ")
          }
          .getOrElse(
            assert.fail(s"'$key' header is missing")
          )
      }
      .combineAll
  }

  object testCase {

    def checkQueryParameters(
        tc: HttpRequestTestCase,
        queryParameters: Map[String, Seq[String]]
    ): ComplianceResult = {
      val existenceChecks = assert.queryParamsExistenceCheck(
        queryParameters = queryParameters,
        requiredParameters = tc.requireQueryParams,
        forbiddenParameters = tc.forbidQueryParams
      )
      val valueChecks =
        assert.queryParamValuesCheck(queryParameters, tc.queryParams)

      existenceChecks |+| valueChecks
    }

    def checkHeaders(
        tc: HttpRequestTestCase,
        headers: Headers
    ): ComplianceResult = {
      val existenceChecks = assert.headersExistenceCheck(
        headers,
        requiredHeaders = tc.requireHeaders,
        forbiddenHeaders = tc.forbidHeaders
      )
      val valueChecks = assert.headerValuesCheck(headers, tc.headers)
      existenceChecks |+| valueChecks
    }

    def checkHeaders(
        tc: HttpResponseTestCase,
        headers: Headers
    ): ComplianceResult = {
      val existenceChecks = assert.headersExistenceCheck(
        headers,
        requiredHeaders = tc.requireHeaders,
        forbiddenHeaders = tc.forbidHeaders
      )
      val valueChecks = assert.headerValuesCheck(headers, tc.headers)
      existenceChecks |+| valueChecks
    }
  }
}
