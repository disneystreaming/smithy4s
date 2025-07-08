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

import smithy4s.{ShapeId, Hints}
import smithy4s.Document._
import software.amazon.smithy.model.shapes.{AbstractShapeBuilder, Shape}
import software.amazon.smithy.utils.ToSmithyBuilder
import software.amazon.smithy.model.traits.Trait
import smithy4s.dynamic.syntax._

private[dynamic] object syntax {
  implicit class ShapeBuilderOps[A <: AbstractShapeBuilder[A, S], S <: Shape](
      builder: AbstractShapeBuilder[A, S]
  ) {
    def setId(sid: ShapeId): A = {
      builder.id(sid.toSmithy)
      builder.asInstanceOf[A]
    }
  }

  implicit class ShapeOps[A <: Shape](val a: A) extends AnyVal {
    def captureHints(hints: Hints): A = addTraits(a, hints)
  }

  def addTraits[A <: Shape](shape: A, hints: Hints): A = {
    shape match {
      case s: ToSmithyBuilder[_] =>
        s.toBuilder match {
          case s: AbstractShapeBuilder[_, _] =>
            s.addTraits(toSmithyTraits(hints))
            s.build().asInstanceOf[A]
          case _ => shape
        }
      case _ => shape
    }
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
