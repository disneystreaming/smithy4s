/*
 *  Copyright 2021-2025 Disney Streaming
 *
 *  Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     https://disneystreaming.github.io/TOST-1.0.txt
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package smithy4s.codegen.cli
import coursier._
import coursier.cache.FileCache
import coursier.parse._
import cats.data.NonEmptyList
import cats.syntax.all._
import com.monovore.decline.Command
import com.monovore.decline.Opts
import smithy4s.codegen.Codegen
import java.net.URLClassLoader

object Main {

  val commands: Command[Smithy4sCommand] =
    Command("smithy4s", "Command line interface for Smithy4s")(
      NonEmptyList
        .of(
          CodegenCommand.command,
          DumpModelCommand.command,
          VersionCommand.command
        )
        .reduceMapK(Opts.subcommand(_))
    )

  def main(args: Array[String]): Unit = {
    val argsArray = args
    val out = System.out
    try {
      commands
        .parse(args.toList)
        .map {
          case Smithy4sCommand.Generate(args) =>
            Console.err.println("fork value: " + args.fork)
            if (args.fork) {
              // todo: less hardcoding
              val cp = resolveDependencies(
                s"com.disneystreaming.smithy4s:smithy4s-codegen-cli_2.13:${BuildInfo.version}" ::
                  args.dependencies,
                args.localJars,
                args.repositories
              )

              val cl = new URLClassLoader(
                cp.map(_.toIO.toURI.toURL).toArray,
                null
              )

              val mainClass = cl.loadClass("smithy4s.codegen.cli.Main")
              val mainMethod = mainClass.getMethod(
                "main",
                classOf[Array[String]]
              )

              val newArgs = argsArray.filterNot(_.contains("--fork"))
              // run main method
              Console.err.println("invoking main")
              mainMethod.invoke(null, newArgs)
              ()
            } else {
              // todo: repeat this and fork handling in other commands
              System.setOut(System.err)
              val res = Codegen.generateToDisk(args)
              if (res.isEmpty) {
                // Printing to stderr because we print generated files path to stdout
                Console.err.println(
                  List(
                    "Nothing was generated. Make sure your targetting Smithy files or folders",
                    "that include Smithy definitions. Otherwise, you can also use",
                    "--dependencies to pull external JARs or use --local-jars to use",
                    "JARs located on your file system."
                  ).mkString(" ")
                )
              }
              res.foreach(out.println)
            }
          case Smithy4sCommand.DumpModel(args) =>
            // todo: support fork mode
            out.println(Codegen.dumpModel(args))

          case Smithy4sCommand.Version =>
            out.println(BuildInfo.version)
        }
        .leftMap { help =>
          System.err.println(help.show)
          sys.exit(1)
        }
        .merge
    } catch {
      case e: Throwable =>
        e.printStackTrace(System.err)
        sys.exit(1)
    } finally {
      System.setErr(out)
    }
  }

  private def resolveDependencies(
      dependencies: List[String],
      localJars: List[os.Path],
      repositories: List[String]
  ): Seq[os.Path] = {
    val maybeRepos = RepositoryParser.repositories(repositories).either
    val maybeDeps = DependencyParser
      .dependencies(
        dependencies,
        defaultScalaVersion = smithy4s.codegen.BuildInfo.scalaBinaryVersion
      )
      .either
    val repos = maybeRepos match {
      case Left(errorMessages) =>
        throw new IllegalArgumentException(
          s"Failed to parse repositories with error: $errorMessages"
        )
      case Right(r) => r
    }
    val deps = maybeDeps match {
      case Left(errorMessages) =>
        throw new IllegalArgumentException(
          s"Failed to parse dependencies with errors: $errorMessages"
        )
      case Right(d) => d
    }
    val resolvedDeps: Seq[os.Path] =
      if (deps.nonEmpty) {
        val fetch = Fetch(FileCache())
          .addRepositories(repos: _*)
          .addDependencies(deps: _*)
        fetch.run().map(os.Path(_))
      } else {
        Seq.empty
      }
    resolvedDeps ++ localJars
  }

}
