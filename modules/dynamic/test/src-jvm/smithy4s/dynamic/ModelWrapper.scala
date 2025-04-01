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

import software.amazon.smithy.model._
import software.amazon.smithy.model.node._
import scala.jdk.CollectionConverters._
import software.amazon.smithy.build.transforms.FilterSuppressions
import software.amazon.smithy.build.TransformContext
import software.amazon.smithy.model.shapes.SmithyIdlModelSerializer
import software.amazon.smithy.model.transform.ModelTransformer
import software.amazon.smithy.model.traits.Trait
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.traits.BoxTrait
import software.amazon.smithy.diff.ModelDiff
import java.util.stream.Collectors

// In order to have nice comparisons from test reports.
class ModelWrapper(val model: Model) {

  private final implicit class JavaStreamOps[A](
      stream: java.util.stream.Stream[A]
  ) {
    def asList: List[A] = stream.collect(Collectors.toList()).asScala.toList
  }

  override def equals(obj: Any): Boolean = obj match {
    case wrapper: ModelWrapper =>
      val one = reorderMetadata(reorderFields(model))
      val two = reorderMetadata(reorderFields(wrapper.model))
      val diff = ModelDiff
        .builder()
        .oldModel(one)
        .newModel(two)
        .compare()
        .getDifferences()
      val added = diff.addedShapes().asList
      val hasChanges =
        diff
          .changedShapes()
          .asList
          .exists { changed =>
            val addedTraits =
              changed.addedTraits().asList
            val removedTraits = changed
              .removedTraits()
              .asList
            val changedTraits = changed
              .changedTraits()
              .asList
              .filterNot { pair =>
                // compare shapeId and node values to avoid issues with differing java classes
                pair.getLeft.toShapeId == pair.getRight.toShapeId && pair.getLeft.toNode == pair.getRight.toNode
              }
              .filterNot { pair =>
                // don't consider synthetic traits
                pair.getLeft().toShapeId().getNamespace() == "smithy.synthetic"
              }
            addedTraits.nonEmpty || removedTraits.nonEmpty || changedTraits.nonEmpty
          }
      val removed =
        diff.removedShapes().asList
      added.isEmpty && !hasChanges && removed.isEmpty
    case _ => false
  }

  private def reorderMetadata(model: Model): Model = {
    implicit val nodeOrd: Ordering[Node] = (x: Node, y: Node) =>
      x.hashCode() - y.hashCode()

    implicit val nodeStringOrd: Ordering[StringNode] = {
      val ord = Ordering[String]
      (x: StringNode, y: StringNode) => ord.compare(x.getValue(), y.getValue())
    }
    def goNode(n: Node): Node = n match {
      case array: ArrayNode =>
        val elements = array.getElements().asScala.toList.sorted
        Node.arrayNode(elements: _*)
      case obj: ObjectNode =>
        Node.objectNode(
          obj.getMembers().asScala.toSeq.sortBy(_._1).toMap.asJava
        )
      case other => other
    }
    def go(metadata: Map[String, Node]): Map[String, Node] = {
      val keys = metadata.keySet.toVector.sorted
      keys.map { k =>
        k -> goNode(metadata(k))
      }.toMap
    }

    val builder = model.toBuilder()
    val newMeta = go(model.getMetadata().asScala.toMap)
    builder.clearMetadata()
    builder.metadata(newMeta.asJava)
    builder.build()
  }

  private val reorderFields: Model => Model = m => {
    val structures = m.getStructureShapes().asScala.map { structShape =>
      val sortedMembers =
        structShape.members().asScala.toList.sortBy(_.getMemberName())
      structShape.toBuilder().members(sortedMembers.asJava).build()
    }
    m.toBuilder().addShapes(structures.asJava).build()
  }

  private def update(model: Model): Model = {
    val filterSuppressions: Model => Model = m =>
      new FilterSuppressions().transform(
        TransformContext
          .builder()
          .model(m)
          .settings(
            ObjectNode.builder().withMember("removeUnused", true).build()
          )
          .build()
      )
    (filterSuppressions andThen reorderFields)(model)
  }

  override def toString() =
    SmithyIdlModelSerializer
      .builder()
      .build()
      .serialize(update(model))
      .asScala
      .map(in => s"${in._1.toString.toUpperCase}:\n\n${in._2}")
      .mkString("\n")
}

object ModelWrapper {
  def apply(model: Model): ModelWrapper = {
    // Remove all box traits because they are applied inconsistently depending on if you
    // load from Java model or from unparsed string model
    @annotation.nowarn("msg=class BoxTrait in package traits is deprecated")
    val noBoxModel = ModelTransformer
      .create()
      .filterTraits(
        model,
        ((_: Shape, trt: Trait) => trt.toShapeId() != BoxTrait.ID)
      )
    new ModelWrapper(noBoxModel)
  }
}
