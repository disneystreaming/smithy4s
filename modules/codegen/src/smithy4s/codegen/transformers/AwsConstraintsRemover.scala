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

package smithy4s.codegen.transformers

import software.amazon.smithy.build.ProjectionTransformer
import software.amazon.smithy.build.TransformContext
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.neighbor.NeighborProvider
import scala.jdk.CollectionConverters._

/** A preprocessor that removes constraints from specifications of AWS. */
class AwsConstraintsRemover extends ProjectionTransformer {

  override def getName: String = AwsConstraintsRemover.name

  private val constraintsToRemove = List("length", "range", "pattern")
  private val awsNamespacePrefix = "com.amazonaws"

  override def transform(context: TransformContext): Model = {
    val transformer = context.getTransformer()
    transformer.removeTraitsIf(
      context.getModel(),
      (shape, t) => {
        val shapeName = t.toShapeId().getName()
        val isOperation = shape.isOperationShape()
        val isInAwsNamespace =
          shape.getId.getNamespace.startsWith(awsNamespacePrefix)

        if (isOperation && isInAwsNamespace ) {
          val np = NeighborProvider.of(context.getModel())
          val neighbors = np.getNeighbors(shape).asScala.toList
        }
        ???
      }
    )
  }
}

object AwsConstraintsRemover {
  val name: String = "AwsConstraintsRemover"

}
