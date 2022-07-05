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

package smithy4s.openapi

import _root_.software.amazon.smithy.model.Model
import weaver._

import scala.io.Source
import scala.util.Using

object OpenApiConversionSpec extends SimpleIOSuite {

  pureTest("OpenAPI conversion from simpleRestJson protocol") {
    val model = Model
      .assembler()
      .addImport(getClass().getClassLoader().getResource("foo.smithy"))
      .discoverModels()
      .assemble()
      .unwrap()

    val result = convert(model, None)
      .map(_.contents)
      .mkString
      .filterNot(_.isWhitespace)

    val expected = Using
      .resource(Source.fromResource("foo.json"))(
        _.getLines().mkString.filterNot(_.isWhitespace)
      )

    expect(result == expected)
  }

  pureTest("OpenAPI conversion from testJson protocol") {
    val model = Model
      .assembler()
      .addImport(getClass().getClassLoader().getResource("baz.smithy"))
      .discoverModels()
      .assemble()
      .unwrap()

    val result = convert(model, None)
      .map(_.contents)
      .mkString
      .filterNot(_.isWhitespace)

    val expected = Using
      .resource(Source.fromResource("baz.json"))(
        _.getLines().mkString.filterNot(_.isWhitespace)
      )
    expect(result == expected)
  }

}
