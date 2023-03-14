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

import ComplianceTest.ComplianceResult
import smithy.test.NonEmptyString
import smithy4s.ShapeId

case class ComplianceTest[F[_]](
    id: String,
    endpoint: ShapeId,
    tags: List[String],
    run: F[ComplianceResult]
) {
  private val showTags =
    if (tags.isEmpty) "" else tags.mkString(" Tags[", ", ", "]")
  def show = s"${endpoint.id}: $id $showTags"
}

object ComplianceTest {
  type ComplianceResult = Either[String, Unit]
  def apply[F[_]](
      id: String,
      endpoint: ShapeId,
      tags: Option[List[NonEmptyString]],
      run: F[ComplianceResult]
  ): ComplianceTest[F] =
    ComplianceTest(
      id,
      endpoint,
      tags.getOrElse(List.empty).map(_.value),
      run
    )
}
