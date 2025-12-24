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

import smithy4s.{Document, ShapeId}
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.shapes.{ShapeId => SmithyShapeId}
import scala.jdk.CollectionConverters._

object syntax {
  final implicit class DocumentOps(private val doc: Document) extends AnyVal {
    def toSmithy: Node = doc match {
      case Document.DString(value)  => Node.from(value)
      case Document.DNumber(value)  => Node.from(value)
      case Document.DBoolean(value) => Node.from(value)
      case Document.DObject(values) =>
        Node.objectNode(values.map { case (k, v) =>
          Node.from(k) -> v.toSmithy
        }.asJava)
      case Document.DArray(values) =>
        Node.fromNodes(values.map(_.toSmithy): _*)
      case Document.DNull => Node.nullNode()
    }
  }

  final implicit class NodeOps(private val node: Node) extends AnyVal {
    def toSmithy4s: Document = NodeToDocument(node)
  }

  final implicit class ShapeIdOps(private val sid: ShapeId) extends AnyVal {
    def toSmithy: SmithyShapeId =
      SmithyShapeId.fromParts(sid.namespace, sid.name)
  }

  final implicit class SmithyShapeIdOps(private val sid: SmithyShapeId)
      extends AnyVal {
    def toSmithy4s: ShapeId = ShapeId(sid.getNamespace, sid.getName)
  }
}
