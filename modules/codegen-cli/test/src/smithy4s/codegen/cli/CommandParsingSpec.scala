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
