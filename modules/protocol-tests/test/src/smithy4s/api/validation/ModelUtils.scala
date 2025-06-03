/*
 *  Copyright 2021-2025 Disney Streaming
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

package smithy4s.api.validation

import software.amazon.smithy.model.Model
import software.amazon.smithy.model.SourceLocation
import software.amazon.smithy.model.validation.ValidatedResult
import software.amazon.smithy.model.validation.ValidationEvent

import scala.jdk.CollectionConverters._

private object ModelUtils {

  def assembleModel(text: String): ValidatedResult[Model] = {
    Model
      .assembler()
      .discoverModels()
      .addUnparsedModel(
        "test.smithy",
        text
      )
      .assemble()
  }

  def eventsWithoutLocations(
      result: ValidatedResult[?]
  ): List[ValidationEvent] = {
    if (!result.isBroken()) sys.error("Expected a broken result")
    result.getValidationEvents.asScala.toList.map(e =>
      e.toBuilder.sourceLocation(SourceLocation.NONE).build()
    )
  }
}
