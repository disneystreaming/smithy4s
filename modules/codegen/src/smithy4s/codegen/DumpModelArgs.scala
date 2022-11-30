package smithy4s.codegen

final case class DumpModelArgs(
    specs: List[os.Path],
    repositories: List[String],
    dependencies: List[String],
    transformers: List[String],
    localJars: List[os.Path]
)
