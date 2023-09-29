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

import software.amazon.smithy.model.shapes.ModelSerializer
import software.amazon.smithy.model.transform.ModelTransformer

private[dynamic] trait DynamicSchemaIndexPlatform {
  self: DynamicSchemaIndex.type =>

  /**
    * Loads a dynamic schema index model from a smithy model.
    */
  def loadModel(
      model: software.amazon.smithy.model.Model
  ): DynamicSchemaIndex = {
    val flattenedModel =
      ModelTransformer.create().flattenAndRemoveMixins(model);
    val node = ModelSerializer.builder().build.serialize(flattenedModel)
    val document = NodeToDocument(node)
    smithy4s.Document
      .decode[smithy4s.dynamic.model.Model](document)
      .map(load(_)) match {
      case Left(error)  => throw error
      case Right(value) => value
    }
  }

}
