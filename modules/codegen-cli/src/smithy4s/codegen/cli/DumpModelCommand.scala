package smithy4s.codegen.cli

import cats.implicits._
import com.monovore.decline.Command

import Options._

object DumpModelCommand {
  import Smithy4sCommand._

  val options = (
    specsArgs,
    repositoriesOpt.map(_.getOrElse(Nil)),
    dependenciesOpt.map(_.getOrElse(Nil))
  ).mapN(DumpModelArgs.apply)

  val command: Command[DumpModel] =
    Command("dump-model", "Output a JSON view of the Smithy models")(
      options.map(DumpModel.apply)
    )
}
