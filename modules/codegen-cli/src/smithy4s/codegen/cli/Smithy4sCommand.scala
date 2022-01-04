package smithy4s.codegen.cli

import smithy4s.codegen.CodegenArgs

sealed trait Smithy4sCommand extends Product with Serializable
object Smithy4sCommand {
  final case class Generate(args: CodegenArgs) extends Smithy4sCommand
  final case class DumpModel(args: DumpModelArgs) extends Smithy4sCommand

  final case class DumpModelArgs(
      specs: List[os.Path],
      repositories: List[String],
      dependencies: List[String]
  )

}
