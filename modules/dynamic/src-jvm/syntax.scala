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

package smithy4s.dynamic

import smithy4s.{Document, ShapeId, Hints}
import smithy4s.Document._
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.shapes.{ShapeId => SmithyShapeId}
import software.amazon.smithy.model.traits.Trait
import scala.jdk.CollectionConverters._

object syntax {
  def documentToNode(doc: Document): Node = doc match {
    case DString(value)  => Node.from(value)
    case DNumber(value)  => Node.from(value)
    case DBoolean(value) => Node.from(value)
    case DObject(values) =>
      Node.objectNode(values.map { case (k, v) =>
        Node.from(k) -> documentToNode(v)
      }.asJava)
    case DArray(values) => Node.fromNodes(values.map(documentToNode): _*)
    case DNull          => Node.nullNode()
  }

  def nodeToDocument(node: Node): Document = NodeToDocument(node)

  implicit class ShapeIdOps(sid: ShapeId) {
    def toSmithy: SmithyShapeId =
      SmithyShapeId.fromParts(sid.namespace, sid.name)
  }

  implicit class SmithyShapeIdOps(sid: SmithyShapeId) {
    def toSmithy4s: ShapeId = ShapeId(sid.getNamespace, sid.getName)
  }

  def toSmithyTraits(hints: Hints): java.util.Collection[Trait] = {
    hints.all.toList
      .map {
        case Hints.Binding.DynamicBinding(keyId, value) =>
          new Trait {
            def toShapeId() =
              SmithyShapeId.fromParts(keyId.namespace, keyId.name)
            def toNode() = documentToNode(value)
          }
        case Hints.Binding.StaticBinding(key, value) =>
          val doc = Document.Encoder.fromSchema(key.schema).encode(value)
          new Trait {
            def toShapeId() =
              SmithyShapeId.fromParts(key.id.namespace, key.id.name)
            def toNode() = documentToNode(doc)
          }
      }
      .filterNot(
        _.toShapeId == SmithyShapeId.fromParts("smithy4s", "InputOutput")
      )
      .asJava
  }

  def toSmithy4sHints(traits: java.util.Collection[Trait]): Hints = {
    Hints(traits.asScala.map { t =>
      Hints.Binding.DynamicBinding(
        ShapeId(t.toShapeId.getNamespace, t.toShapeId.getName),
        nodeToDocument(t.toNode)
      )
    }.toSeq: _*)
  }
}
