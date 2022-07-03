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

package smithy4s.api.validation

import smithy4s.api.SimpleRestJsonTrait
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.pattern.UriPattern
import software.amazon.smithy.model.shapes._
import software.amazon.smithy.model.traits.HttpTrait
import software.amazon.smithy.model.validation._

import scala.jdk.CollectionConverters._

object SimpleRestJsonValidationSpec extends weaver.FunSuite {

  private def validator = new SimpleRestJsonValidator()

  test(
    "validation events are returned when operations are missing http trait"
  ) {
    val op = OperationShape
      .builder()
      .id("test#op")
      .input(StructureShape.builder().id("test#struct").build())
      .build()

    val service = ServiceShape
      .builder()
      .id("test#serv")
      .version("1")
      .addTrait(new SimpleRestJsonTrait())
      .addOperation(op)
      .build()

    val model = Model.builder().addShape(service).addShape(op).build()

    val result = validator.validate(model).asScala.toList
    val expected = List(
      ValidationEvent
        .builder()
        .id("SimpleRestJson")
        .shape(op)
        .severity(Severity.ERROR)
        .message(
          "Operations tied to smithy4s.api#simpleRestJson services must be annotated with the @http trait"
        )
        .build()
    )
    expect.same(result, expected)
  }

  test(
    "no events are returned when operations have http trait"
  ) {

    val httpTrait = HttpTrait
      .builder()
      .code(200)
      .method("POST")
      .uri(UriPattern.parse("/test"))
      .build()
    val op = OperationShape
      .builder()
      .id("test#op")
      .input(StructureShape.builder().id("test#struct").build())
      .addTrait(httpTrait)
      .build()

    val service = ServiceShape
      .builder()
      .id("test#serv")
      .version("1")
      .addTrait(new SimpleRestJsonTrait())
      .addOperation(op)
      .build()

    val model = Model.builder().addShape(service).addShape(op).build()

    val result = validator.validate(model).asScala.toList
    val expected = List.empty
    expect.same(result, expected)
  }

  test(
    "validation events are not returned when service is not simpleRestJson"
  ) {
    val op = OperationShape
      .builder()
      .id("test#op")
      .input(StructureShape.builder().id("test#struct").build())
      .build()

    val service = ServiceShape
      .builder()
      .id("test#serv")
      .version("1")
      .addOperation(op)
      .build()

    val model = Model.builder().addShape(service).addShape(op).build()

    val result = validator.validate(model).asScala.toList
    val expected = List.empty
    expect.same(result, expected)
  }

  test(
    "Validator is wired to the jvm Service mechanism"
  ) {
    val modelString =
      """|namespace foo
         |
         |use smithy4s.api#simpleRestJson
         |
         |@simpleRestJson
         |service HelloService {
         |  version : "1",
         |  operations : [Greet]
         |}
         |
         |operation Greet {
         |}
         |
         |""".stripMargin

    val events = Model
      .assembler(this.getClass().getClassLoader())
      .discoverModels()
      .addUnparsedModel("foo.smithy", modelString)
      .assemble()
      .getValidationEvents()
      .asScala
      .filter(_.getSeverity() == Severity.ERROR)
      .toList

    expect(events.size == 1)
  }

}
