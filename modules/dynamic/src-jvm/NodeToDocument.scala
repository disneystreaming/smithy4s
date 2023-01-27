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

package smithy4s.dynamic

import smithy4s.Document
import software.amazon.smithy.model.node._

import scala.jdk.CollectionConverters._

object NodeToDocument {

  def apply(node: Node): Document =
    return node.accept(new NodeVisitor[Document] {
      def arrayNode(x: ArrayNode): Document =
        Document.array(x.getElements().asScala.map(_.accept(this)))

      def booleanNode(x: BooleanNode): Document =
        Document.fromBoolean(x.getValue())

      def nullNode(x: NullNode): Document =
        Document.DNull

      def numberNode(x: NumberNode): Document =
        Document.fromDouble(x.getValue().doubleValue())

      def objectNode(x: ObjectNode): Document =
        Document.obj(
          x.getMembers()
            .asScala
            .map { case (key, value) =>
              key.getValue() -> value.accept(this)
            }
            .toSeq: _*
        )

      def stringNode(x: StringNode): Document =
        Document.fromString(x.getValue())
    })

}
