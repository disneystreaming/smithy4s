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

package smithy4s.codegen

import software.amazon.smithy.model.Model
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.SourceLocation

import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

private[codegen] final case class CodegenRecord(
    namespaces: List[String],
    validatedNewtypes: Option[Boolean],
    source: SourceLocation
)

private[codegen] object CodegenRecord {

  val METADATA_KEY = "smithy4sGenerated"

  def recordsFromModel(model: Model): List[CodegenRecord] =
    model
      .getMetadata()
      .asScala
      .get(METADATA_KEY)
      .map(_.expectArrayNode().getElements().asScala.map(fromNode))
      .map(_.toList)
      .getOrElse(List.empty)

  def fromNode(node: Node): CodegenRecord = {
    val obj = node.expectObjectNode()
    val arrayNode = obj.expectArrayMember("namespaces")
    val validatedNewtypes =
      obj.getBooleanMember("validatedNewtypes").toScala.map(_.getValue())
    val namespaces = arrayNode
      .getElements()
      .asScala
      .map(_.expectStringNode().getValue())
      .toList
    CodegenRecord(namespaces, validatedNewtypes, node.getSourceLocation())
  }
}
