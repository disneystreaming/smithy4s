package smithy4s.codegen.cli
import cats.data.Validated
import cats.data.ValidatedNel
import cats.syntax.all._
import com.monovore.decline.Argument
import com.monovore.decline.Opts

import java.nio.file

object Options {

  implicit val osPathArg: Argument[os.Path] = new Argument[os.Path] {
    def defaultMetavar: String = "path"
    def read(string: String): ValidatedNel[String, os.Path] =
      implicitly[Argument[file.Path]].read(string).andThen { path =>
        try {
          if (path.isAbsolute()) Validated.validNel(os.Path(path))
          else Validated.validNel(os.pwd / os.RelPath(path))
        } catch {
          case e: Throwable =>
            Validated.invalidNel(e.getMessage() + ":" + string)
        }
      }

  }

  val specsArgs = Opts
    .arguments[os.Path]()
    .mapValidated(
      _.traverse(path =>
        if (os.exists(path)) Validated.valid(path)
        else Validated.invalidNel(s"$path does not exist")
      )
    )
    .orNone
    .map {
      case Some(value) => value.toList
      case None        => List.empty
    }

  val repositoriesOpt: Opts[Option[List[String]]] =
    Opts
      .option[String](
        "repositories",
        "Comma-delimited list of repositories to look in for resolving any provided dependencies"
      )
      .map(_.split(',').toList)
      .orNone

  val dependenciesOpt: Opts[Option[List[String]]] =
    Opts
      .option[String](
        "dependencies",
        "Comma-delimited list of dependencies containing smithy files"
      )
      .map(_.split(',').toList)
      .orNone

}
