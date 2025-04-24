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

package smithy4s.codegen.transformers

import software.amazon.smithy.build.ProjectionTransformer
import software.amazon.smithy.build.TransformContext
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.neighbor._
import software.amazon.smithy.model.shapes._

import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

class KeepOnlyMarkedShapes extends ProjectionTransformer {

  override def getName: String = KeepOnlyMarkedShapes.name

  private val annotationName = "smithy4s.meta#only"
  private val protectedNamespaces = Set("smithy.api")

  private def log(msg: => Any) = {
    // Uncomment to actually see the logs
    // System.err.println(
    //   s"[KeepOnlyMarkedShapes]: $msg"
    // )

    ()
  }

  override def transform(ctx: TransformContext): Model = {
    val operationsToKeepBuilder = collection.mutable.Set.empty[ShapeId]
    val operationsToRemoveBuilder = collection.mutable.Set.empty[ShapeId]

    val taggedOperations = ctx
      .getModel()
      .getShapesWithTrait(ShapeId.from(annotationName))
      .asScala
      .flatMap(_.asOperationShape().toScala.map(_.getId()))

    operationsToKeepBuilder ++= taggedOperations

    val model = ctx.getModel()

    model
      .getServiceShapes()
      .asScala
      .foreach { service =>
        val operationIds = service.getOperations().asScala.toSet
        val serviceTaggedOperations =
          operationIds.filter(taggedOperations.contains)
        log(s"${service.getId()}: ${serviceTaggedOperations}")

        if (serviceTaggedOperations.nonEmpty) {
          operationsToRemoveBuilder ++= operationIds -- serviceTaggedOperations
        } else {
          // pretend that all service's operations were tagged
          operationsToKeepBuilder ++= operationIds
        }
      }

    log(s"Operations to keep: $operationsToKeepBuilder")
    log(s"Operations to remove: $operationsToRemoveBuilder")

    val walker = new Walker(model)
    val removalCandidates = operationsToRemoveBuilder.flatMap { id =>
      model.getShape(id).toScala.toSet.flatMap { (shape: Shape) =>
        val closure = walker
          .walkShapeIds(shape)
          .asScala
          .filterNot(ns => protectedNamespaces.contains(ns.getNamespace()))

        log(s"Removed operation $id closure: $closure")

        closure
      }
    }
    log(s"Removal candidates (before filtering): $removalCandidates")

    operationsToKeepBuilder.foreach { oid =>
      model.getShape(oid).toScala.foreach { shape =>
        val closure = walker.walkShapeIds(shape).asScala
        log(s"Kept operation $oid closure: $closure")
        removalCandidates --= closure
      }
    }

    log(s"Removal candidates (after filtering): $removalCandidates")

    ctx
      .getTransformer()
      .removeShapes(
        ctx.getModel(),
        removalCandidates.flatMap(model.getShape(_).toScala).asJava
      )
  }
}

object KeepOnlyMarkedShapes {
  val name: String = "KeepOnlyMarkedShapes"
}
