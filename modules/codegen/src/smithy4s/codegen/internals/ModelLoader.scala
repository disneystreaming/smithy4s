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

package smithy4s.codegen
package internals

import software.amazon.smithy.build.ProjectionTransformer
import software.amazon.smithy.build.TransformContext
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.loader.ModelAssembler

import java.io.File

private[codegen] object ModelLoader {

  def load(
      specs: Set[File],
      transformers: List[String]
  ): Model = {
    val preTransformationModel = Model
      .assembler(this.getClass.getClassLoader)
      .discoverModels(this.getClass.getClassLoader)
      .addImports(specs)
      .assemble()
      .unwrap()

    val transformerFactory =
      ProjectionTransformer.createServiceFactory(this.getClass.getClassLoader)

    val trans = transformers.flatMap {
      transformerFactory(_).asScala
    }

    val transformedModel = trans.foldLeft(preTransformationModel)((m, t) =>
      t.transform(TransformContext.builder().model(m).build())
    )

    val postTransformationModel = Model
      .assembler(this.getClass.getClassLoader)
      .addModel(transformedModel)
      .assemble()
      .unwrap

    postTransformationModel
  }

  implicit class ModelAssemblerOps(assembler: ModelAssembler) {
    def addImports(files: Set[java.io.File]): ModelAssembler = {
      files.map(_.toPath()).foreach(assembler.addImport)
      assembler
    }

    def addImports(urls: Seq[java.net.URL]): ModelAssembler = {
      urls.foreach(assembler.addImport)
      assembler
    }

  }

}
