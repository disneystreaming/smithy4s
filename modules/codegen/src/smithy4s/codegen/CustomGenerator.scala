package smithy4s.codegen

import software.amazon.smithy.model.Model

trait CustomGenerator {
  def generate(
      cu: CompilationUnit,
      model: Model
  ): List[CustomGenerator.ScalaSourceFile]
}

object CustomGenerator {
  case class ScalaSourceFile(
      smithyNamespace: String,
      basename: String,
      content: String
  ) {
    private[codegen] def asRendererResult: Renderer.Result = Renderer.Result(
      namespace = smithyNamespace,
      name = basename,
      content = content
    )
  }
}
