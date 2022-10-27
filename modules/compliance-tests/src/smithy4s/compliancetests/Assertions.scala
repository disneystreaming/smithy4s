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

import cats.implicits._
import ComplianceTest._
import org.http4s.Headers
import org.typelevel.ci.CIString
import smithy.test.{HttpResponseTestCase, HttpRequestTestCase}

case class ComplianceTest[F[_]](name: String, run: F[ComplianceResult])

object ComplianceTest {
  type ComplianceResult = Either[String, Unit]
}

object assert {
  def success: ComplianceResult = Right(())
  def fail(msg: String): ComplianceResult = Left(msg)

  def eql[A](expected: A, actual: A): ComplianceResult = {
    if (expected == actual) {
      success
    } else {
      fail(s"Actual value: $actual was not equal to $expected.")
    }
  }

  private def headersExistenceCheck(
      headers: Headers,
      expected: Either[Option[List[String]], Option[List[String]]]
  ) = {
    expected match {
      case Left(forbidHeaders) =>
        forbidHeaders.toList.flatten.collect {
          case key if headers.get(CIString(key)).isDefined =>
            assert.fail(s"Header $key is forbidden in the request.")
        }.combineAll
      case Right(requireHeaders) =>
        requireHeaders.toList.flatten.collect {
          case key if headers.get(CIString(key)).isEmpty =>
            assert.fail(s"Header $key is required request.")
        }.combineAll
    }
  }
  private def headersCheck(
      headers: Headers,
      expected: Option[Map[String, String]]
  ) = {
    expected.toList
      .flatMap(_.toList)
      .map { case (key, expectedValue) =>
        headers
          .get(CIString(key))
          .map { v =>
            assert.eql[String](expectedValue, v.head.value)
          }
          .getOrElse(
            assert.fail(s"'$key' header is missing")
          )
      }
      .combineAll
  }

  object testCase {
    def checkHeaders(
        tc: HttpRequestTestCase,
        headers: Headers
    ): ComplianceResult = {
      assert.headersExistenceCheck(headers, Left(tc.forbidHeaders)) *>
        assert.headersExistenceCheck(headers, Right(tc.requireHeaders)) *>
        assert.headersCheck(headers, tc.headers)
    }

    def checkHeaders(
        tc: HttpResponseTestCase,
        headers: Headers
    ): ComplianceResult = {
      assert.headersExistenceCheck(headers, Left(tc.forbidHeaders)) *>
        assert.headersExistenceCheck(headers, Right(tc.requireHeaders)) *>
        assert.headersCheck(headers, tc.headers)
    }
  }
}
