package smithy4s.codegen

import sjsonnew._
import BasicJsonProtocol._
import sbt.FileInfo
import sbt.HashFileInfo
import sjsonnew._

private[smithy4s] object JsonConverters {

  implicit val pathFormat: JsonFormat[os.Path] = {
    val hashInfoFormat = implicitly[JsonFormat[HashFileInfo]]
    new JsonFormat[os.Path] {
      def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): os.Path = {
        os.Path(hashInfoFormat.read(jsOpt, unbuilder).file)
      }

      def write[J](obj: os.Path, builder: Builder[J]): Unit =
        hashInfoFormat.write(FileInfo.hash(obj.toIO), builder)
    }
  }

  // format: off
  type GenTarget = List[os.Path] :*: os.Path :*: os.Path :*: Boolean :*: Boolean :*: Boolean :*: Option[Set[String]] :*: Option[Set[String]] :*: List[String] :*: List[String] :*: List[String] :*: List[os.Path] :*: LNil
  // format: on
  implicit val codegenArgsIso = LList.iso[CodegenArgs, GenTarget](
    { ca: CodegenArgs =>
      ("specs", ca.specs) :*:
        ("output", ca.output) :*:
        ("openapiOutput", ca.openapiOutput) :*:
        ("skipScala", ca.skipScala) :*:
        ("skipOpenapi", ca.skipOpenapi) :*:
        ("discoverModels", ca.discoverModels) :*:
        ("allowedNS", ca.allowedNS) :*:
        ("excludedNS", ca.excludedNS) :*:
        ("repositories", ca.repositories) :*:
        ("dependencies", ca.dependencies) :*:
        ("transformers", ca.transformers) :*:
        ("localJars", ca.localJars) :*:
        LNil
    },
    {
      case (_, specs) :*:
          (_, output) :*:
          (_, openapiOutput) :*:
          (_, skipScala) :*:
          (_, skipOpenapi) :*:
          (_, discoverModels) :*:
          (_, allowedNS) :*:
          (_, excludedNS) :*:
          (_, repositories) :*:
          (_, dependencies) :*:
          (_, transformers) :*:
          (_, localJars) :*: LNil =>
        CodegenArgs(
          specs,
          output,
          openapiOutput,
          skipScala,
          skipOpenapi,
          discoverModels,
          allowedNS,
          excludedNS,
          repositories,
          dependencies,
          transformers,
          localJars
        )
    }
  )

}
