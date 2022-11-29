package smithy4s.codegen

object Codegen {

  def processSpecs(
      args: CodegenArgs
  ): Set[os.Path] = internals.CodegenImpl.processSpecs(args)

}
