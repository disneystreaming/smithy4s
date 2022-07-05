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

import model.Model
import software.amazon.smithy.model.{Model => SModel}
import software.amazon.smithy.model.shapes.ModelSerializer
import cats.syntax.all._
import cats.effect.IO

private[dynamic] trait PlatformUtils { self: Utils.type =>

  def compile(string: String): IO[DynamicSchemaIndex] =
    parse(string).map(self.compile)

  def compileSampleSpec(string: String): IO[DynamicSchemaIndex] =
    parseSampleSpec(string).map(self.compile)

  def parse(string: String): IO[Model] =
    IO(
      SModel
        .assembler()
        .addUnparsedModel("dynamic.smithy", string)
        .assemble()
        .unwrap()
    ).map(ModelSerializer.builder().build.serialize(_))
      .map(NodeToDocument(_))
      .map(smithy4s.Document.decode[smithy4s.dynamic.model.Model](_))
      .flatMap(_.liftTo[IO])

  def parseSampleSpec(fileName: String): IO[Model] =
    IO(
      SModel
        .assembler()
        .addImport(s"./sampleSpecs/$fileName")
        .addImport(
          "./modules/protocol/resources/META-INF/smithy/smithy4s.smithy"
        )
        .discoverModels()
        .assemble()
        .unwrap()
    ).map(ModelSerializer.builder().build.serialize(_))
      .map(NodeToDocument(_))
      .map(smithy4s.Document.decode[smithy4s.dynamic.model.Model](_))
      .flatMap(_.liftTo[IO])

}
