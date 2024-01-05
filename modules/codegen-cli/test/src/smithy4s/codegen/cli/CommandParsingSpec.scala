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

package smithy4s.codegen.cli
import smithy4s.codegen.CodegenArgs
import smithy4s.codegen.DumpModelArgs
import smithy4s.codegen.FileType
import weaver._

import Defaults.defaultDependencies

object CommandParsingSpec extends FunSuite {

  test("parsing empty `generate` call") {
    assert(
      Main.commands.parse(List("generate")) ==
        Right(
          Smithy4sCommand.Generate(
            CodegenArgs(
              specs = Nil,
              output = os.pwd,
              resourceOutput = os.pwd,
              skip = Set.empty,
              discoverModels = false,
              allowedNS = None,
              excludedNS = None,
              repositories = Nil,
              dependencies = defaultDependencies,
              transformers = Nil,
              localJars = Nil
            )
          )
        )
    )
  }
  test("parsing `generate` call with all parameters") {
    val result = Main.commands.parse(
      List(
        "generate",
        "sampleSpecs/pizza.smithy",
        "sampleSpecs/example.smithy",
        "--output",
        "target",
        "--resource-output",
        "target/openapi",
        "--skip",
        "scala",
        "--skip",
        "openapi",
        "--allowed-ns",
        "name1,name2",
        "--repositories",
        "repo1,repo2",
        "--dependencies",
        "dep1,dep2",
        "--transformers",
        "t1,t2",
        "--local-jars",
        "lib1.jar,lib2.jar"
      )
    )

    assert(
      result ==
        Right(
          Smithy4sCommand.Generate(
            CodegenArgs(
              specs = List(
                os.pwd / "sampleSpecs" / "pizza.smithy",
                os.pwd / "sampleSpecs" / "example.smithy"
              ),
              output = os.pwd / "target",
              resourceOutput = os.pwd / "target" / "openapi",
              skip = Set(FileType.Openapi, FileType.Scala),
              discoverModels = false,
              allowedNS = Some(Set("name1", "name2")),
              excludedNS = None,
              repositories = List("repo1", "repo2"),
              dependencies = defaultDependencies ++ List("dep1", "dep2"),
              transformers = List("t1", "t2"),
              localJars = List(
                os.pwd / "lib1.jar",
                os.pwd / "lib2.jar"
              )
            )
          )
        )
    )
  }

  test("parsing empty `dump-model` call") {
    assert(
      Main.commands.parse(List("dump-model")) ==
        Right(
          Smithy4sCommand.DumpModel(
            DumpModelArgs(
              specs = Nil,
              repositories = Nil,
              dependencies = Nil,
              transformers = Nil,
              localJars = Nil
            )
          )
        )
    )
  }
  test("parsing `dump-model` call with all parameters") {
    val result = Main.commands.parse(
      List(
        "dump-model",
        "sampleSpecs/pizza.smithy",
        "sampleSpecs/example.smithy",
        "--repositories",
        "repo1,repo2",
        "--dependencies",
        "dep1,dep2",
        "--transformers",
        "t1,t2",
        "--local-jars",
        "lib1.jar,lib2.jar"
      )
    )
    assert(
      result ==
        Right(
          Smithy4sCommand.DumpModel(
            DumpModelArgs(
              specs = List(
                os.pwd / "sampleSpecs" / "pizza.smithy",
                os.pwd / "sampleSpecs" / "example.smithy"
              ),
              repositories = List("repo1", "repo2"),
              dependencies = List("dep1", "dep2"),
              transformers = List("t1", "t2"),
              localJars = List(
                os.pwd / "lib1.jar",
                os.pwd / "lib2.jar"
              )
            )
          )
        )
    )
  }
}
