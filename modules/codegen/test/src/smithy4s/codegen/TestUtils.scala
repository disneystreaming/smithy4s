package smithy4s.codegen

import munit.{Location, Assertions}
import software.amazon.smithy.model.Model

object TestUtils {

  def runTest(
      smithySpec: String,
      expectedScalaCode: String
  )(implicit
      loc: Location
  ): Unit = {
    val model = Model
      .assembler()
      .discoverModels()
      .addUnparsedModel("foo.smithy", smithySpec)
      .assemble()
      .unwrap()

    val results = Codegen.generate(model, None, None)
    val scalaResults = results.map { case (_, _, contents) => contents }
    Assertions.assertEquals(scalaResults, List(expectedScalaCode))
  }

}
