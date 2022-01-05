/*
 *  Copyright 2021 Disney Streaming
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
import weaver._
import smithy4s.codegen.CodegenArgs

object CommandParsingSpec extends FunSuite {

  test("parsing empty `generate` call") {
    assert(
      Main.commands.parse(List("generate")) ==
        Right(
          Smithy4sCommand.Generate(
            CodegenArgs(
              specs = Nil,
              output = os.pwd,
              openapiOutput = os.pwd,
              skipScala = false,
              skipOpenapi = false,
              allowedNS = None,
              repositories = Nil,
              dependencies = Nil
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
        "--openapi-output",
        "target/openapi",
        "--skip-scala",
        "--skip-openapi",
        "--allowed-ns",
        "name1,name2",
        "--repositories",
        "repo1,repo2",
        "--dependencies",
        "dep1,dep2"
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
              openapiOutput = os.pwd / "target" / "openapi",
              skipScala = true,
              skipOpenapi = true,
              allowedNS = Some(Set("name1", "name2")),
              repositories = List("repo1", "repo2"),
              dependencies = List("dep1", "dep2")
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
            Smithy4sCommand.DumpModelArgs(
              specs = Nil,
              repositories = Nil,
              dependencies = Nil
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
        "dep1,dep2"
      )
    )
    assert(
      result ==
        Right(
          Smithy4sCommand.DumpModel(
            Smithy4sCommand.DumpModelArgs(
              specs = List(
                os.pwd / "sampleSpecs" / "pizza.smithy",
                os.pwd / "sampleSpecs" / "example.smithy"
              ),
              repositories = List("repo1", "repo2"),
              dependencies = List("dep1", "dep2")
            )
          )
        )
    )
  }
}
