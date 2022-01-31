/*
 *  Copyright 2021 Disney Streaming
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

import _root_.software.amazon.smithy.jsonschema.JsonSchemaMapper
import _root_.software.amazon.smithy.openapi.fromsmithy.OpenApiJsonSchemaMapper
import _root_.software.amazon.smithy.openapi.fromsmithy.OpenApiMapper
import _root_.software.amazon.smithy.openapi.fromsmithy.OpenApiProtocol
import _root_.software.amazon.smithy.openapi.fromsmithy.Smithy2OpenApiExtension
import _root_.software.amazon.smithy.openapi.fromsmithy.mappers._

import java.{util => ju}
import scala.jdk.CollectionConverters._

final class Smithy4sOpenApiExtension() extends Smithy2OpenApiExtension {

  override def getProtocols(): ju.List[OpenApiProtocol[_]] = List(
    new Smithy4sOpenApiProtocol()
  ).asJava.asInstanceOf[ju.List[OpenApiProtocol[_]]]

  override def getOpenApiMappers(): ju.List[OpenApiMapper] = List(
    new CheckForGreedyLabels(),
    new CheckForPrefixHeaders(),
    new OpenApiJsonSubstitutions(),
    new OpenApiJsonAdd(),
    new RemoveUnusedComponents(),
    new UnsupportedTraits(),
    new RemoveEmptyComponents(),
    new AddTags()
  ).asJava

  override def getJsonSchemaMappers(): ju.List[JsonSchemaMapper] = List(
    new OpenApiJsonSchemaMapper(): JsonSchemaMapper,
    new DiscriminatedUnions()
  ).asJava

}
