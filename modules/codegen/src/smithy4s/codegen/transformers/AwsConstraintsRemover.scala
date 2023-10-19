/*
 *  Copyright 2021-2023 Disney Streaming
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
import software.amazon.smithy.model.neighbor.NeighborProvider
import software.amazon.smithy.model.neighbor.Walker
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.traits.LengthTrait
import software.amazon.smithy.model.traits.PatternTrait
import software.amazon.smithy.model.traits.RangeTrait

import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

/** A preprocessor that removes constraints from specifications of AWS. */
class AwsConstraintsRemover extends ProjectionTransformer {

  override def getName: String = AwsConstraintsRemover.name

  private val traitToRemove =
    Set(LengthTrait.ID, RangeTrait.ID, PatternTrait.ID)
  private val awsNamespacePrefix = "com.amazonaws"

  override def transform(context: TransformContext): Model = {
    val model = context.getModel()
    val walker: Walker = new Walker(
      NeighborProvider.cached(NeighborProvider.of(model))
    )
    val awsOperations = model
      .getOperationShapes()
      .asScala
      .filter(_.getId().getNamespace().startsWith(awsNamespacePrefix))

    val awsOutputShapes =
      awsOperations.flatMap(_.getOutput().toScala.toSet).map(model.expectShape)
    val awsOutputConnectedShapes: Set[ShapeId] =
      awsOutputShapes.flatMap(walker.walkShapeIds(_).asScala).toSet

    val transformer = context.getTransformer()
    transformer.removeTraitsIf(
      context.getModel(),
      (shape, t) => {
        traitToRemove(t.toShapeId) && awsOutputConnectedShapes(shape.getId())
      }
    )
  }
}

object AwsConstraintsRemover {
  val name: String = "AwsConstraintsRemover"

}
