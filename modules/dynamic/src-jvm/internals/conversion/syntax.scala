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

package smithy4s.dynamic.internals.conversion

import smithy4s.ShapeId
import software.amazon.smithy.model.shapes.{ShapeId => SmithyShapeId, _}
import smithy4s.Hints
import software.amazon.smithy.utils.ToSmithyBuilder
import smithy4s.Document
import smithy4s.Document._
import software.amazon.smithy.model.traits.Trait
import scala.jdk.CollectionConverters._
import software.amazon.smithy.model.node.Node

private[dynamic] object syntax {
  implicit class ShapeIdOps(sid: ShapeId) {
    def toSmithy: SmithyShapeId =
      SmithyShapeId.fromParts(sid.namespace, sid.name)
  }

  implicit class ShapeBuilderOps[A <: AbstractShapeBuilder[
    A,
    S
  ], S <: Shape](
      builder: AbstractShapeBuilder[A, S]
  ) {
    def setId(sid: ShapeId): A = {
      builder.id(sid.toSmithy)
      builder.asInstanceOf[A]
    }
  }

  private def documentToNode(doc: Document): Node = doc match {
    case DString(value)  => Node.from(value)
    case DNumber(value)  => Node.from(value)
    case DBoolean(value) => Node.from(value)
    case DObject(values) =>
      Node.objectNode(values.map { case (key, value) =>
        Node.from(key) -> documentToNode(value)
      }.asJava)
    case DArray(values) => Node.fromNodes(values.map(documentToNode): _*)
    case DNull          => Node.nullNode()
  }

  implicit class ShapeOps[A <: Shape](val a: A) extends AnyVal {
    def captureHints(hints: Hints): A = addTraits(a, hints)
  }

  def smithyTrait(id: ShapeId, document: Document): Trait = new Trait {
    def toShapeId() = SmithyShapeId.fromParts(id.namespace, id.name)
    def toNode() = documentToNode(document)
  }

  implicit class HintsOpts(val hints: Hints) extends AnyVal {
    def asTraits: java.util.Collection[Trait] = {
      hints.all.toList
        .map {
          case Hints.Binding.DynamicBinding(keyId, value) =>
            smithyTrait(keyId, value)
          case Hints.Binding.StaticBinding(key, value) =>
            val doc = Document.Encoder.fromSchema(key.schema).encode(value)
            smithyTrait(key.id, doc)
        }
        .filterNot(in =>
          in.toShapeId() == SmithyShapeId.fromParts("smithy4s", "InputOutput")
        )
        .asJava
    }
  }

  def addTraits[A <: Shape](shape: A, hints: Hints): A = {
    shape match {
      case s: ToSmithyBuilder[_] =>
        s.toBuilder match {
          case s: AbstractShapeBuilder[_, _] =>
            s.addTraits(hints.asTraits)
            s.build().asInstanceOf[A]
          case _ => shape
        }
      case _ => shape
    }
  }
}
