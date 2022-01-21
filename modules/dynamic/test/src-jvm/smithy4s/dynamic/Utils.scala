package smithy4s.dynamic

import model.Model
import software.amazon.smithy.model.{Model => SModel}
import software.amazon.smithy.model.shapes.ModelSerializer
import cats.syntax.all._
import cats.effect.IO

object Utils {

  def compile(string: String): IO[DynamicModel] =
    parse(string).map(compile)

  def compile(model: Model): DynamicModel =
    Compiler.compile(model)

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

}
