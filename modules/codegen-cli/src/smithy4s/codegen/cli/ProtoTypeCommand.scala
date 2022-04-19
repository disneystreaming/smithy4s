package smithy4s.codegen.cli

import cats.data.Validated
import cats.syntax.all._
import com.monovore.decline.Command
import com.monovore.decline.Opts
import os.Path
import smithy4s.codegen.ProtoTypeArgs
import smithy4s.codegen.cli.Smithy4sCommand.ProtoType

object ProtoTypeCommand {

  import Options._

  val smithyString =
    Opts.option[String](long = "input", help = "Smithy string ")
  val outputOpt: Opts[Option[Path]] =
    Opts
      .option[os.Path](
        long = "output",
        help = "Path where scala code should be written to. Defaults to pwd",
        short = "o"
      )
      .mapValidated(path =>
        if (os.exists(path) && !os.isDir(path))
          Validated.invalidNel(s"$path is not a directory")
        else Validated.valid(path)
      )
      .orNone

  val options: Opts[ProtoTypeArgs] =
    (smithyString, outputOpt)
      .mapN {
          // format: off
          case (smithy, outputDir) =>
            // format: on
          ProtoTypeArgs(
            smithy,
            outputDir.getOrElse(os.pwd)
          )
      }

  val command: Command[ProtoType] =
    Command(
      "proto",
      "generate code from a command line input for the purposes of learning and testing smithy4s functionality"
    )(
      options.map(ProtoType.apply)
    )

}
