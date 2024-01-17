/*
 *  Copyright 2021-2024 Disney Streaming
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

import software.amazon.smithy.model.{Model => SModel}
import software.amazon.smithy.model.loader.ModelAssembler
import DummyIO._

private[dynamic] trait PlatformUtils { self: Utils.type =>

  def compile(string: String): IO[DynamicSchemaIndex] =
    parse(string).map(DynamicSchemaIndex.loadModel)

  def compileSampleSpec(string: String): IO[DynamicSchemaIndex] =
    parseSampleSpec(string)
      .map(DynamicSchemaIndex.loadModel)

  private def parse(string: String): IO[SModel] =
    IO(
      SModel
        .assembler()
        .addUnparsedModel("dynamic.smithy", string)
        .addImport(
          // Alloy open enums
          getClass().getClassLoader.getResource("META-INF/smithy/enums.smithy")
        )
        .assemble()
        .unwrap()
    )

  private def parseSampleSpec(fileName: String): IO[SModel] =
    IO(
      SModel
        .assembler()
        .addImport(s"./sampleSpecs/$fileName")
        .discoverModels(this.getClass().getClassLoader())
        .putProperty(ModelAssembler.DISABLE_JAR_CACHE, true)
        .assemble()
        .unwrap()
    )

}
