package smithy4s.codegen.cli

import smithy4s.codegen.ModelLoader
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.shapes.ModelSerializer

object DumpModel {
  def run(args: Smithy4sCommand.DumpModelArgs): String = {
    val (_, model) = ModelLoader.load(
      args.specs.map(_.toIO).toSet,
      args.dependencies,
      args.repositories
    )

    Node.prettyPrintJson(ModelSerializer.builder().build.serialize(model))
  }
}
