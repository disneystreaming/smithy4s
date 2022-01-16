package smithy4s.dynamic.model

import weaver._
import software.amazon.smithy.model.{Model => SModel}
import software.amazon.smithy.model.shapes.ModelSerializer
import cats.effect.IO
import cats.syntax.all._
import java.nio.file.Files
import java.nio.file.Paths

object DynamicModelPizzaSpec extends SimpleIOSuite {

  val modelString = new String(
    Files.readAllBytes(
      // quick and dirty
      Paths.get("../../../sampleSpecs/pizza.smithy").toAbsolutePath()
    )
  )

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
        .addUnparsedModel("pizza.smithy", modelString)
        .assemble()
        .unwrap()
    ).map(ModelSerializer.builder().build.serialize(_))
      .map(NodeToDocument(_))
      .map(smithy4s.Document.decode[smithy4s.dynamic.model.Model](_))
      .flatMap(_.liftTo[IO])
      .map(obtained => expect.same(obtained, expected))
  }

}
