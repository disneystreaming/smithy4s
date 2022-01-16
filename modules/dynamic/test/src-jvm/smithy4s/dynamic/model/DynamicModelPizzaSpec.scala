package smithy4s.dynamic.model

import weaver._
import software.amazon.smithy.model.{Model => SModel}
import software.amazon.smithy.model.shapes.ModelSerializer
import cats.effect.IO
import cats.syntax.all._
import java.nio.file.Paths

object DynamicModelPizzaSpec extends SimpleIOSuite {

  // This is not ideal, but it does the job.
  val cwd = System.getProperty("user.dir");
  val pizzaSpec = Paths.get(cwd + "/sampleSpecs/pizza.smithy").toAbsolutePath()
  val smithy4sPrelude = Paths
    .get(cwd + "/modules/protocol/resources/META-INF/smithy/smithy4s.smithy")
    .toAbsolutePath()

  val expected: Model = {
    Model(
      smithy = Some("1.0"),
      shapes = Map(
      )
    )
  }

  test("Decode json representation of models") {
    // Unable to resolve trait `smithy4s.api#simpleRestJson`. If this is a custom trait, then it must be defined before it can be used in a model.
    IO(
      SModel
        .assembler()
        .addImport(smithy4sPrelude)
        .addImport(pizzaSpec)
        .assemble()
        .unwrap()
    ).map(ModelSerializer.builder().build.serialize(_))
      .map(NodeToDocument(_))
      .map(smithy4s.Document.decode[smithy4s.dynamic.model.Model](_))
      .flatMap(_.liftTo[IO])
      .map(obtained => expect.same(obtained, expected))
  }

}
