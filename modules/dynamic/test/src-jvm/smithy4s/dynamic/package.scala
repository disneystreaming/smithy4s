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

package smithy4s

import smithy4s.dynamic.DummyIO._
import java.nio.file.Paths
import software.amazon.smithy.model.{Model => SModel}
import software.amazon.smithy.model.shapes.ModelSerializer
import cats.syntax.all._

package object dynamic {
  // This is not ideal, but it does the job.
  private lazy val cwd = System.getProperty("user.dir");

  def loadDynamicModel(specName: String) =
    IO {
      val spec = Paths.get(cwd + s"/sampleSpecs/$specName").toAbsolutePath()
      SModel
        .assembler()
        .discoverModels()
        .addImport(spec)
        .assemble()
        .unwrap()
    }
      .map(ModelSerializer.builder().build.serialize(_))
      .map(NodeToDocument(_))
      .map(smithy4s.Document.decode[smithy4s.dynamic.model.Model](_))
      .flatMap(_.liftTo[IO])

}
