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

package smithy4s.codegen.cli

import smithy4s.codegen.ModelLoader
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.shapes.ModelSerializer

object DumpModel {
  def run(args: Smithy4sCommand.DumpModelArgs): String = {
    val (_, model) = ModelLoader.load(
      args.specs.map(_.toIO).toSet,
      args.dependencies,
      args.repositories,
      args.transformers,
      discoverModels = false,
      args.localJars
    )

    Node.prettyPrintJson(ModelSerializer.builder().build.serialize(model))
  }
}
